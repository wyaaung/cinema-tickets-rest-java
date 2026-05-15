package uk.gov.dwp.engineering.recruitment.service;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

@Service
public class TicketRequestAggregatorImpl implements TicketRequestAggregator {

  @Override
  public TicketTally aggregate(final TicketRequest... ticketRequests) {
    return null; // TODO: implement this method
  }
}
