package uk.gov.dwp.engineering.recruitment.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TicketPriceCalculatorImpl implements TicketPriceCalculator {

  @Override
  public BigDecimal totalCost(final TicketTally tally) {
    return new BigDecimal(0L); // Todo: implement this method to calculate the total cost of the booking based on the ticket tally
  }
}
