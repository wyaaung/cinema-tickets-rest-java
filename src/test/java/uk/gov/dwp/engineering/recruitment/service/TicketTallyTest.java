package uk.gov.dwp.engineering.recruitment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TicketTally — aggregated ticket counts; infants excluded from seat count")
class TicketTallyTest {

  @Test
  @DisplayName("total() sums adults + children + infants")
  void givenTallyWithAllTypes_whenTotal_thenSumOfAllCountsReturned() {
    final TicketTally tally = new TicketTally(2, 3, 1);
    assertEquals(6, tally.total());
  }

  @Test
  @DisplayName("seatCount() excludes infants")
  void givenTallyWithInfants_whenSeatCount_thenInfantsExcluded() {
    final TicketTally tally = new TicketTally(2, 3, 5);
    assertEquals(5, tally.seatCount());
  }

  @Test
  @DisplayName("Empty tally totals and seats are zero")
  void givenEmptyTally_whenTotalAndSeatCount_thenZeroReturned() {
    final TicketTally tally = new TicketTally(0, 0, 0);
    assertEquals(0, tally.total());
    assertEquals(0, tally.seatCount());
  }

  @Test
  @DisplayName("Infant-only tally has no seats")
  void givenInfantOnlyTally_whenSeatCount_thenZeroReturned() {
    final TicketTally tally = new TicketTally(0, 0, 3);
    assertEquals(3, tally.total());
    assertEquals(0, tally.seatCount());
  }
}
