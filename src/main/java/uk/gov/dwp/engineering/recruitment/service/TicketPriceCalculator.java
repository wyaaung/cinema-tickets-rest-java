package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;

/**
 * Computes the total monetary cost of a booking from an already-aggregated {@link TicketTally}.
 *
 * <p>Implementations are responsible for the unit price of each {@code TicketType}; the tally is
 * trusted to be valid (per upstream {@link TicketRequestValidator}).
 */
public interface TicketPriceCalculator {
  BigDecimal totalCost(TicketTally tally);
}
