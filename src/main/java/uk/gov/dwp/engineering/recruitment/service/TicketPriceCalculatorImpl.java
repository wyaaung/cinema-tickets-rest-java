package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

/**
 * Computes the total cost of a {@link TicketTally} by multiplying each ticket count by its
 * unit price (looked up in {@link TicketPrices#BY_TYPE}) and summing the results.
 *
 * <p>This class owns only the calculation logic. The price data lives in {@link TicketPrices}
 * so prices can be edited without touching the calculator. The tally is trusted to be
 * non-negative; upstream validation in {@link TicketRequestValidator} and the compact
 * constructor on {@link TicketTally} enforce that.
 */
@Service
public class TicketPriceCalculatorImpl implements TicketPriceCalculator {

  @Override
  public BigDecimal totalCost(final TicketTally tally) {
    return Arrays.stream(TicketType.values()).map(
        type -> TicketPrices.BY_TYPE.get(type).multiply(BigDecimal.valueOf(tally.countFor(type))))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
