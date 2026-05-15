package uk.gov.dwp.engineering.recruitment.service;

public record TicketTally(int adults, int children, int infants) {

  public int total() {
    return adults + children + infants;
  }

  public int seatCount() {
    return adults + children;
  }
}
