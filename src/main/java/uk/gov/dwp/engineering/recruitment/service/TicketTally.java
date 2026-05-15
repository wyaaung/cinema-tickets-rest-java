package uk.gov.dwp.engineering.recruitment.service;

public record TicketTally(int adults, int children, int infants) {

  public int total() {
    return 0;
  }

  public int seatCount() {
    return 0;
  }
}
