package uk.gov.dwp.engineering.recruitment.service;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

/**
 * Applies every business and structural rule that governs a ticket purchase request.
 *
 * <p>The validation is performed in two phases: structural checks on the raw inputs
 * (account, array, per-element) followed by business-rule checks on the aggregated tally
 * (total bounds, accompanying-adult rule). On success the aggregated {@link TicketTally} is
 * returned so callers do not need to re-aggregate; on failure an {@link InvalidBookingException}
 * is thrown with a message identifying the broken rule.
 */
public interface TicketRequestValidator {

  TicketTally validate(Long accountId, TicketRequest... ticketRequests) throws InvalidBookingException;
}
