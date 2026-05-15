package uk.gov.dwp.engineering.recruitment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  @Test
  @DisplayName("Negative adult count rejected at construction")
  void givenNegativeAdultCount_whenConstructed_thenIllegalArgumentExceptionThrown() {
    final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> new TicketTally(-1, 0, 0));
    assertEquals("ticket counts must be non-negative", thrown.getMessage());
  }

  @Test
  @DisplayName("Negative child count rejected at construction")
  void givenNegativeChildCount_whenConstructed_thenIllegalArgumentExceptionThrown() {
    assertThrows(IllegalArgumentException.class, () -> new TicketTally(0, -1, 0));
  }

  @Test
  @DisplayName("Negative infant count rejected at construction")
  void givenNegativeInfantCount_whenConstructed_thenIllegalArgumentExceptionThrown() {
    assertThrows(IllegalArgumentException.class, () -> new TicketTally(0, 0, -1));
  }

  @Test
  @DisplayName("countFor(type) returns the count for each ticket type")
  void givenTally_whenCountForEachType_thenCorrectCountReturned() {
    final TicketTally tally = new TicketTally(2, 3, 1);
    assertEquals(2, tally.countFor(TicketType.ADULT));
    assertEquals(3, tally.countFor(TicketType.CHILD));
    assertEquals(1, tally.countFor(TicketType.INFANT));
  }
}
