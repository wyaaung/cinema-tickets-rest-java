package uk.gov.dwp.engineering.recruitment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

@DisplayName("TicketRequestAggregatorImpl — sums TicketRequest entries into a TicketTally")
class TicketRequestAggregatorImplTest {

  private final TicketRequestAggregator aggregator = new TicketRequestAggregatorImpl();

  @Test
  @DisplayName("Single adult request → tally(1, 0, 0)")
  void givenSingleAdultRequest_whenAggregate_thenSingleAdultTallyReturned() {
    final TicketTally tally = aggregator.aggregate(new TicketRequest(ADULT, 1));
    assertEquals(new TicketTally(1, 0, 0), tally);
  }

  @Test
  @DisplayName("Mixed-type requests are summed per type")
  void givenMixedRequests_whenAggregate_thenCountsSummedPerType() {
    final TicketTally tally = aggregator.aggregate(
        new TicketRequest(ADULT, 1),
        new TicketRequest(ADULT, 2),
        new TicketRequest(CHILD, 3),
        new TicketRequest(INFANT, 1)
    );

    assertEquals(new TicketTally(3, 3, 1), tally);
  }

  @Test
  @DisplayName("Zero-count entry contributes nothing to the tally")
  void givenRequestWithZeroCount_whenAggregate_thenZeroContributesNothing() {
    final TicketTally tally = aggregator.aggregate(
        new TicketRequest(ADULT, 1),
        new TicketRequest(CHILD, 0)
    );

    assertEquals(new TicketTally(1, 0, 0), tally);
  }

  @Test
  @DisplayName("Empty array → empty tally")
  void givenEmptyRequestArray_whenAggregate_thenEmptyTallyReturned() {
    final TicketTally tally = aggregator.aggregate();
    assertEquals(new TicketTally(0, 0, 0), tally);
  }

  @Test
  @DisplayName("Multiple requests of the same type are summed")
  void givenMultipleRequestsOfSameType_whenAggregate_thenCountsSummed() {
    final TicketTally tally = aggregator.aggregate(
        new TicketRequest(INFANT, 1),
        new TicketRequest(INFANT, 2),
        new TicketRequest(INFANT, 3)
    );

    assertEquals(new TicketTally(0, 0, 6), tally);
  }
}
