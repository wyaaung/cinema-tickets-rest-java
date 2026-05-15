package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

@Service
public class TicketPriceCalculatorImpl implements TicketPriceCalculator {

  @Override
  public BigDecimal totalCost(final TicketTally tally) {
    return Arrays.stream(TicketType.values()).map(
        type -> TicketPrices.BY_TYPE.get(type).multiply(BigDecimal.valueOf(tally.countFor(type))))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
