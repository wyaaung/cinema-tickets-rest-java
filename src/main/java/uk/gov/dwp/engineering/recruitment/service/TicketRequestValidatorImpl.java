package uk.gov.dwp.engineering.recruitment.service;

import java.util.Objects;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

/**
 * Two-phase booking-request validation.
 *
 * <p>Phase 1 (pre-aggregation) checks the raw inputs — account id, array structure, and
 * per-element shape — and short-circuits with {@link InvalidBookingException} <em>before</em>
 * the aggregator is invoked. Phase 2 (post-aggregation) delegates to
 * {@link TicketRequestAggregator} to compute a {@link TicketTally} and then applies the
 * business rules: at least one ticket, total within the maximum, and accompanying-adult.
 *
 * <p>The aggregator is constructor-injected (DIP) and is documented to assume pre-validated
 * input — that contract is upheld by running phase 1 first. On success the validated tally
 * is returned so the orchestrator does not need to re-aggregate.
 */
@Service
public class TicketRequestValidatorImpl implements TicketRequestValidator {

  static final int MAX_TICKETS_PER_PURCHASE = 25;

  static final String MSG_INVALID_ACCOUNT_ID = "accountId must be a positive value";
  static final String MSG_REQUESTS_EMPTY = "at least one ticket request is required";
  static final String MSG_NULL_ELEMENT = "ticket requests must not contain null entries";
  static final String MSG_NULL_TYPE = "ticket request type must not be null";
  static final String MSG_NEGATIVE_COUNT = "ticket count must not be negative";
  static final String MSG_NO_TICKETS = "at least one ticket must be purchased";
  static final String MSG_EXCEEDS_MAX = String.format(
      "cannot purchase more than %d tickets in a single transaction", MAX_TICKETS_PER_PURCHASE);
  static final String MSG_NO_ADULT =
      "child and infant tickets require at least one accompanying adult";

  private final TicketRequestAggregator aggregator;

  public TicketRequestValidatorImpl(final TicketRequestAggregator aggregator) {
    this.aggregator = Objects.requireNonNull(aggregator, "aggregator");
  }

  @Override
  public TicketTally validate(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException {
    requireValidAccountId(accountId);
    requireNonEmptyRequests(ticketRequests);
    requireWellFormedElements(ticketRequests);

    final TicketTally tally = aggregator.aggregate(ticketRequests);

    requireAtLeastOneTicket(tally);
    requireWithinMaximum(tally);
    requireAccompanyingAdult(tally);

    return tally;
  }

  private static void requireValidAccountId(final Long accountId) {
    if (accountId == null || accountId <= 0L) {
      throw new InvalidBookingException(MSG_INVALID_ACCOUNT_ID);
    }
  }

  private static void requireNonEmptyRequests(final TicketRequest[] ticketRequests) {
    if (ticketRequests == null || ticketRequests.length == 0) {
      throw new InvalidBookingException(MSG_REQUESTS_EMPTY);
    }
  }

  private static void requireWellFormedElements(final TicketRequest[] ticketRequests) {
    for (final TicketRequest request : ticketRequests) {
      if (request == null) {
        throw new InvalidBookingException(MSG_NULL_ELEMENT);
      }
      if (request.type() == null) {
        throw new InvalidBookingException(MSG_NULL_TYPE);
      }
      if (request.ticketCount() < 0) {
        throw new InvalidBookingException(MSG_NEGATIVE_COUNT);
      }
    }
  }

  private static void requireAtLeastOneTicket(final TicketTally tally) {
    if (tally.total() == 0) {
      throw new InvalidBookingException(MSG_NO_TICKETS);
    }
  }

  private static void requireWithinMaximum(final TicketTally tally) {
    if (tally.total() > MAX_TICKETS_PER_PURCHASE) {
      throw new InvalidBookingException(MSG_EXCEEDS_MAX);
    }
  }

  private static void requireAccompanyingAdult(final TicketTally tally) {
    if (tally.adults() == 0 && (tally.children() > 0 || tally.infants() > 0)) {
      throw new InvalidBookingException(MSG_NO_ADULT);
    }
  }
}
