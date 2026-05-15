package uk.gov.dwp.engineering.recruitment.service;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

public interface TicketRequestValidator {

  TicketTally validate(Long accountId, TicketRequest... ticketRequests) throws InvalidBookingException;
}
