package uk.gov.dwp.engineering.recruitment.service;

import uk.gov.dwp.engineering.recruitment.domain.TicketType;

public record TicketTally(int adults, int children, int infants) {

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
