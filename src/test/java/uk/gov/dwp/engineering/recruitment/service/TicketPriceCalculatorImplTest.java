package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("TicketPriceCalculatorImpl — sums total cost from an aggregated TicketTally")
class TicketPriceCalculatorImplTest {

  private final TicketPriceCalculator calculator = new TicketPriceCalculatorImpl();

  @Test
  @DisplayName("Adult-only tally → adults × £25")
  void givenAdultOnlyTally_whenTotalCost_thenAdultPricePerTicketReturned() {
    final TicketTally tally = new TicketTally(3, 0, 0);
    assertEquals(0, BigDecimal.valueOf(75).compareTo(calculator.totalCost(tally)));
  }

  @Test
  @DisplayName("Child-only tally → children × £15")
  void givenChildOnlyTally_whenTotalCost_thenChildPricePerTicketReturned() {
    final TicketTally tally = new TicketTally(0, 4, 0);
    assertEquals(0, BigDecimal.valueOf(60).compareTo(calculator.totalCost(tally)));
  }

  @Test
  @DisplayName("Infant-only tally → £0")
  void givenInfantOnlyTally_whenTotalCost_thenZeroReturned() {
    final TicketTally tally = new TicketTally(0, 0, 5);
    assertEquals(0, BigDecimal.ZERO.compareTo(calculator.totalCost(tally)));
  }

  @Test
  @DisplayName("Mixed tally → sum of per-type prices")
  void givenMixedTally_whenTotalCost_thenSumOfTypePricesReturned() {
    final TicketTally tally = new TicketTally(2, 3, 1);
    assertEquals(0, BigDecimal.valueOf(95).compareTo(calculator.totalCost(tally)));
  }

  @Test
  @DisplayName("Empty tally → £0")
  void givenEmptyTally_whenTotalCost_thenZeroReturned() {
    final TicketTally tally = new TicketTally(0, 0, 0);
    assertEquals(0, BigDecimal.ZERO.compareTo(calculator.totalCost(tally)));
  }

  @Test
  @DisplayName("Maximum allowed tally (25 tickets) → correct sum")
  void givenMaximumAllowedTally_whenTotalCost_thenCorrectSumReturned() {
    final TicketTally tally = new TicketTally(13, 12, 0);
    final BigDecimal expected = BigDecimal.valueOf(13L * 25L + 12L * 15L);
    assertEquals(0, expected.compareTo(calculator.totalCost(tally)));
  }
}
