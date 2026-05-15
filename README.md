# Cinema Tickets Code Test

A Spring Boot 4 / Java 21 implementation of the DWP cinema tickets recruitment exercise. The `CinemaTicketsService` accepts variadic `TicketRequest`s for an account, enforces every business rule from the brief, prices the order, takes payment via the (external) `PaymentService`, reserves seats via the (external) `SeatReservationService`, and returns a structured booking reference. Invalid requests are rejected with `InvalidBookingException` **before** any external side effect.

## Ticket prices

| Ticket type | Price |
| --- | --- |
| INFANT | £0 |
| CHILD | £15 |
| ADULT | £25 |

INFANT tickets pay nothing and are not allocated a seat — the infant sits on an accompanying adult's lap. The unit prices live in `TicketPrices` (`ADULT`, `CHILD`, `INFANT` constants plus a `BY_TYPE` lookup map), kept separate from `TicketPriceCalculatorImpl` so a price change is a one-file edit. The map is built with `Map.of(...)` and the constants use `BigDecimal.valueOf(long)` to avoid `new BigDecimal(double)` precision loss.

## Business rules coverage

Each rule in the brief is enforced by a specific component and exercised by a specific test.

| Brief rule | Where it's enforced | Where it's tested |
| --- | --- | --- |
| 3 ticket types (INFANT, CHILD, ADULT) | `domain/TicketType` (frozen by the brief) | covered transitively in every test |
| Prices INFANT £0 / CHILD £15 / ADULT £25 | `TicketPriceCalculatorImpl` constructor | `TicketPriceCalculatorImplTest` (6 cases) |
| Multiple tickets per purchase | varargs `TicketRequest...` on `purchaseTickets`; aggregator sums same-type entries | `TicketRequestAggregatorImplTest#givenMultipleRequestsOfSameType_…` and `…givenMixedRequests_…` |
| Maximum 25 tickets per purchase | `requireWithinMaximum` in validator | `postAggregationRejection_throws[aggregate total exceeds max (26)]`, `givenMaximumAllowedTotal_…`, `givenOneBelowMaximumTotal_…` |
| INFANT does not pay | `BigDecimal.ZERO` price in `TicketPriceCalculatorImpl` | `givenInfantOnlyTally_whenTotalCost_thenZeroReturned` |
| INFANT is not allocated a seat (on adult's lap) | `TicketTally.seatCount() = adults + children` | `TicketTallyTest#givenInfantOnlyTally_…`, `CinemaTicketsServiceImplTest#givenAdultAndInfantPurchase_…thenInfantExcludedFromSeatCount` |
| CHILD or INFANT requires an ADULT | `requireAccompanyingAdult` in validator | `postAggregationRejection_throws[child only, no adult / infant only, no adult / child and infant, no adult]` |
| Correct amount sent to `PaymentService` | orchestrator calls `paymentService.debitAccount(accountId, total)` | `CinemaTicketsServiceImplTest` — `ArgumentCaptor<BigDecimal>` asserts exact value |
| Correct seat count sent to `SeatReservationService` | orchestrator calls `seatReservationService.reserveSeats(accountId, (long) tally.seatCount())` | `CinemaTicketsServiceImplTest` — `ArgumentCaptor<Long>` asserts exact value, infants excluded |
| Invalid requests rejected with `InvalidBookingException` (before any external call) | validator throws; orchestrator never reaches payment/reservation | `givenInvalidPurchase_whenPurchaseTickets_thenNoPaymentOrReservation` (`verifyNoInteractions` on downstream) |
| `CinemaTicketsService` interface MUST NOT be modified | unchanged on disk (file timestamp 12 May 2026) | n/a |
| `domain/` package MUST NOT be modified | unchanged on disk (`Booking.java`, `TicketRequest.java`, `TicketType.java` all timestamp 12 May 2026) | n/a |

## Build and run

Maven is invoked directly — no wrapper is committed. Java 21 is required (the parent is `spring-boot-starter-parent` 4.0.6).

```bash
mvn clean verify                                           # full build + tests
mvn test                                                   # unit tests only
mvn -Dtest=CinemaTicketsServiceImplTest test               # single class
mvn -Dtest=TicketRequestValidatorImplTest#preAggregationRejection_throwsAndSkipsAggregator test
mvn spring-boot:run                                        # boot the app
```

The full test suite is 51 tests across 12 classes; `mvn clean verify` runs them all in ~5 seconds on a recent laptop.

## Approach

The orchestrator (`CinemaTicketsServiceImpl`) does nothing but sequence four collaborators, each of which has exactly one reason to change:

```text
purchaseTickets(accountId, requests)
    → TicketRequestValidator.validate(...)        → TicketTally
    → TicketPriceCalculator.totalCost(tally)      → BigDecimal
    → PaymentService.debitAccount(...)            (external)
    → SeatReservationService.reserveSeats(...)    (external)
    → "BOOKING-<uuid>|account=<id>|cost=<£>|seats=<n>"
```

The validator itself delegates aggregation to a separate `TicketRequestAggregator` so the validation surface (one class, ~10 rules) stays separable from the counting surface.

### SOLID

-   **S — Single Responsibility.** One reason to change per class:
    -   `TicketTally` — counting model
    -   `TicketRequestAggregator` — aggregation algorithm
    -   `TicketRequestValidator` — booking rules
    -   `TicketPriceCalculator` — pricing model
    -   `CinemaTicketsServiceImpl` — orchestration sequence
-   **O — Open/Closed.** New ticket prices: edit the relevant constant in `TicketPrices` (and add it to `TicketPrices.BY_TYPE`); the calculator does not change. New validation rule: add a `requireX(...)` private method to `TicketRequestValidatorImpl` and call it from `validate(...)`. Existing rule code is untouched in both cases.
-   **L — Liskov Substitution.** Every concrete impl sits behind an interface with the same pre/post-conditions; the orchestrator works against the abstractions and is impl-agnostic.
-   **I — Interface Segregation.** One small interface per role — `validate`, `aggregate`, `totalCost`. No god-facade, no method a caller doesn't need.
-   **D — Dependency Inversion.** `CinemaTicketsServiceImpl` is constructor-injected with **interfaces** for all four collaborators. Mockito mocks substitute cleanly in the unit test — proof the abstraction is real.

## Validation rules

Every invalid case throws `InvalidBookingException` (subclass of `RuntimeException`) with a rule-specific message exposed via `getMessage()`. Each message lives behind a `MSG_*` constant on `TicketRequestValidatorImpl`, so the tests assert against the same constant the production code throws — no string duplication, and a message tweak is a one-line edit. The validator runs in two phases: pre-aggregation rules short-circuit before the aggregator is invoked; post-aggregation rules act on the computed `TicketTally`. Both phases are tested by parameterized methods in `TicketRequestValidatorImplTest`; the labels below are the per-case names visible in the test report.

**Pre-aggregation rejections** — covered by `preAggregationRejection_throwsAndSkipsAggregator`:

| Rule | Reject when | Message constant | Parameterized case label |
| --- | --- | --- | --- |
| Valid account | `accountId == null` | `MSG_INVALID_ACCOUNT_ID` | `null accountId` |
| Valid account | `accountId == 0` | `MSG_INVALID_ACCOUNT_ID` | `zero accountId` |
| Valid account | `accountId < 0` | `MSG_INVALID_ACCOUNT_ID` | `negative accountId` |
| At least one request | `ticketRequests == null` (explicit varargs null) | `MSG_REQUESTS_EMPTY` | `null ticketRequests array` |
| At least one request | `ticketRequests` is empty | `MSG_REQUESTS_EMPTY` | `empty ticketRequests array` |
| No null entries | Any array element is `null` | `MSG_NULL_ELEMENT` | `array contains null element` |
| Well-formed entries | `request.type() == null` | `MSG_NULL_TYPE` | `element with null type` |
| Well-formed entries | `request.ticketCount() < 0` | `MSG_NEGATIVE_COUNT` | `element with negative count` |

**Post-aggregation rejections** — covered by `postAggregationRejection_throws`:

| Rule | Reject when | Message constant | Parameterized case label |
| --- | --- | --- | --- |
| Maximum 25 tickets | `tally.total() > 25` | `MSG_EXCEEDS_MAX` | `aggregate total exceeds max (26)` |
| At least one ticket | `tally.total() == 0` | `MSG_NO_TICKETS` | `aggregate total is zero` |
| Accompanying adult | Children present with zero adults | `MSG_NO_ADULT` | `child only, no adult` |
| Accompanying adult | Infants present with zero adults | `MSG_NO_ADULT` | `infant only, no adult` |
| Accompanying adult | Children + infants present with zero adults | `MSG_NO_ADULT` | `child and infant, no adult` |

## Assumptions and design decisions

-   **Account validity.** `accountId` must be non-null and strictly positive (`> 0`) per the brief. Valid accounts are assumed solvent, so no balance check happens locally.
-   **Lap rule — not 1:1.** The brief says infants "will be sitting on an ADULT lap", not that each infant requires a dedicated adult. Enforcing `infants <= adults` would invent a rule beyond the spec, so the validator only requires `adults >= 1` when children or infants are present. (A test, `givenMoreInfantsThanAdults_whenValidate_thenLapRuleNotEnforcedAndTallyReturned`, locks this decision in.)
-   **`ticketCount == 0` per request — allowed.** The existing `TicketRequestTest` constructs `new TicketRequest(INFANT, 0)`, signalling that zero per request is record-valid. The validator rejects only **negative** counts at element level and **aggregate-zero** at the tally level. This lets clients mix zero-count entries (e.g. dynamic form output) without bespoke client-side filtering.
-   **`ticketRequests` passed as explicit `(TicketRequest[]) null`** is rejected (covered by the `null ticketRequests array` case of `preAggregationRejection_throwsAndSkipsAggregator`).
-   **Pay then reserve, sequentially.** Conventional commerce ordering: take money first, then commit inventory. The brief declares both services defect-free, so no compensation logic is built. A production system would add idempotency keys on the payment side and a saga / outbox on the reservation side to handle the (out-of-scope) partial-failure cases.
-   **Return value.** A structured string of the form `BOOKING-<uuid>|account=<id>|cost=<£>|seats=<n>` — chosen over a bare UUID for log/audit traceability and over echoing third-party response bodies for stability. The format is asserted by `CinemaTicketsServiceImplTest` with a strict regex.
-   **Pricing precision.** `BigDecimal` end-to-end, constructed via `BigDecimal.valueOf(long)` to avoid `new BigDecimal(double)` precision loss. Test assertions use `BigDecimal#compareTo`, not `equals`, to avoid scale-trap false negatives.
-   **`CinemaTicketsController`** is intentionally a stub. The brief specifies that the controller does not need to be implemented for this iteration.

## Concurrency and performance

### Thread-safety today

All four `@Service` beans are Spring singletons shared across request threads. The current implementation is safe by construction:

-   `CinemaTicketsServiceImpl`, `TicketRequestValidatorImpl`, `TicketRequestAggregatorImpl` — no mutable instance state; only `final` constructor-injected collaborator references.
-   `TicketPriceCalculatorImpl` — the `pricesByType` field is built in the constructor and frozen via `Map.copyOf(...)`, which returns an immutable map. Concurrent reads are safe.
-   `TicketTally` is an immutable record. The compact constructor's non-negative check runs once at construction; the accessors are stateless.

No shared mutable state exists in the service layer, so no `synchronized` block, `ReentrantLock`, or `volatile` is needed in the current code.

### Cost shape per call

Each `purchaseTickets` call performs:

-   One array pass for element validation (O(n), n ≤ 25).
-   One stream collect into a 3-entry `EnumMap` for aggregation (O(n)).
-   Three price lookups in `TicketPrices.BY_TYPE`, three `BigDecimal` multiplications, two additions.
-   One `UUID.randomUUID()` plus a five-part string concatenation for the booking reference.
-   Two external HTTP calls — `PaymentService.debitAccount` then `SeatReservationService.reserveSeats`, sequentially.

Local CPU and allocations are microseconds. End-to-end latency is dominated by the two remote calls; tuning the in-memory work has effectively zero impact on observed latency or throughput.

### Out of scope but flagged for production

The brief assumes defect-free external services and one purchase at a time per account, so none of the items below are implemented. Listed in the order they would land if scope expanded:

1. **Observe first.** Actuator + Micrometer timers on the two third-party calls; HTTP read/connect timeouts and a sized connection pool; Tomcat thread-pool tuning. Always do this batch (and a load test against stubbed externals) before reaching for anything below.
2. **Driven by what the metrics show.** Per-account serial guard (`ConcurrentMap<Long, ReentrantLock>` for single-instance, or a distributed lock); idempotency keys at the controller layer; Resilience4j bulkheads per external call; saga/outbox to handle payment-succeeded-but-reservation-failed; parallel pay + reserve only if a refund-on-failure compensation step exists.
3. **Only if RPS demands it.** Rewrite to Spring WebFlux + `WebClient` for non-blocking I/O.

## Testing strategy

-   **TDD.** Each unit was driven by a test class first — `TicketTallyTest`, `TicketRequestAggregatorImplTest`, `TicketRequestValidatorImplTest`, `TicketPriceCalculatorImplTest`, `CinemaTicketsServiceImplTest`.
-   **BDD naming.** Methods follow the existing `givenX_whenY_thenZ` convention so each test reads as a scenario.
-   **Isolation by mocking.** The validator test uses a mocked `TicketRequestAggregator` to drive each tally-level rule deterministically and to prove that pre-aggregation failures short-circuit before the aggregator is invoked (`verifyNoInteractions(aggregator)`). The service test mocks all four collaborators.
-   **Ordering and arguments.** The service test uses Mockito `InOrder` to lock in validate → calculate → pay → reserve, and `ArgumentCaptor` to assert the exact `BigDecimal` payment amount and `Long` seat count — including that infants are excluded from the seat count.
-   **Failure short-circuit.** `givenInvalidPurchase_whenPurchaseTickets_thenNoPaymentOrReservation` proves that an `InvalidBookingException` from the validator stops the orchestrator before payment or reservation.
-   **Spring context smoke test.** `CinemaTicketsApplicationTest` loads the full context, so every new `@Service` bean is exercised for wiring on each build.
