package uk.gov.dwp.engineering.recruitment.service;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

public interface TicketRequestAggregator {
  TicketTally aggregate(TicketRequest... ticketRequests);
}
