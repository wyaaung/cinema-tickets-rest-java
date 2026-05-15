package uk.gov.dwp.engineering.recruitment.service;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

@Service
public class TicketRequestValidatorImpl implements TicketRequestValidator {

  public TicketRequestValidatorImpl() {
  }

  @Override
  public TicketTally validate(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException {
    return null; // TODO implement me
  }
}
