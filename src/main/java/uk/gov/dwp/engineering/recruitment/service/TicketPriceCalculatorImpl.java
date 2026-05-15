package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

@Service
public class TicketPriceCalculatorImpl implements TicketPriceCalculator {

  private static final BigDecimal ADULT_PRICE = BigDecimal.valueOf(25);
  private static final BigDecimal CHILD_PRICE = BigDecimal.valueOf(15);
  private static final BigDecimal INFANT_PRICE = BigDecimal.ZERO;

  public static final Map<TicketType, BigDecimal> TYPE_PRICE = Map.of(
      TicketType.ADULT, ADULT_PRICE,
      TicketType.CHILD, CHILD_PRICE,
      TicketType.INFANT, INFANT_PRICE);

  @Override
  public BigDecimal totalCost(final TicketTally tally) {
    return Arrays.stream(TicketType.values())
        .map(type -> TYPE_PRICE.get(type).multiply(BigDecimal.valueOf(tally.countFor(type))))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
