package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TicketPriceCalculatorImpl implements TicketPriceCalculator {

  private static final BigDecimal ADULT_PRICE = BigDecimal.valueOf(25);
  private static final BigDecimal CHILD_PRICE = BigDecimal.valueOf(15);
  private static final BigDecimal INFANT_PRICE = BigDecimal.ZERO;

  @Override
  public BigDecimal totalCost(final TicketTally tally) {
    return ADULT_PRICE.multiply(BigDecimal.valueOf(tally.adults()))
        .add(CHILD_PRICE.multiply(BigDecimal.valueOf(tally.children())))
        .add(INFANT_PRICE.multiply(BigDecimal.valueOf(tally.infants())));
  }
}
