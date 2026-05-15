package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import java.util.Map;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

/**
 * Unit prices for each {@link TicketType}, in GBP.
 *
 * <p>Held separately from {@link TicketPriceCalculatorImpl} so the data ("what the prices
 * are") is editable independently of the logic ("how the prices are applied"). To change a
 * price, edit the constant here and recompile; no other class needs to change.
 *
 * <p>Built with {@link BigDecimal#valueOf(long)} to avoid the precision loss of
 * {@code new BigDecimal(double)}.
 */
public final class TicketPrices {

  public static final BigDecimal ADULT = BigDecimal.valueOf(25L);
  public static final BigDecimal CHILD = BigDecimal.valueOf(15L);
  public static final BigDecimal INFANT = BigDecimal.ZERO;

  public static final Map<TicketType, BigDecimal> BY_TYPE = Map.of(
      TicketType.ADULT, ADULT,
      TicketType.CHILD, CHILD,
      TicketType.INFANT, INFANT);

  private TicketPrices() {
  }
}
