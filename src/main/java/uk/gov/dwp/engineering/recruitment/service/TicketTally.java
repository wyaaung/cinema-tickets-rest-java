package uk.gov.dwp.engineering.recruitment.service;

import uk.gov.dwp.engineering.recruitment.domain.TicketType;

/**
 * Aggregated counts for a single booking, broken down by ticket category.
 *
 * <p>This value object is the boundary between request aggregation and the downstream pricing /
 * seat reservation steps. INFANT tickets contribute to {@link #total()} but are excluded from
 * {@link #seatCount()} because infants sit on an adult's lap.
 */
public record TicketTally(int adults, int children, int infants) {

  public TicketTally {
    if (adults < 0 || children < 0 || infants < 0) {
      throw new IllegalArgumentException("ticket counts must be non-negative");
    }
  }

  public int total() {
    return adults + children + infants;
  }

  public int seatCount() {
    return adults + children;
  }

  public int countFor(final TicketType type) {
    return switch (type) {
      case ADULT -> adults;
      case CHILD -> children;
      case INFANT -> infants;
    };
  }
}
