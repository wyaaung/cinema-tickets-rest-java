package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;

public interface TicketPriceCalculator {
  BigDecimal totalCost(TicketTally tally);
}
