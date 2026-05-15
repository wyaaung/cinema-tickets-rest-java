package uk.gov.dwp.engineering.recruitment.service;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

/**
 * Aggregates a collection of {@link TicketRequest} entries (potentially containing several
 * requests of the same {@code TicketType}) into a single {@link TicketTally}.
 *
 * <p>Implementations assume the input array and all elements are non-null and that each
 * {@code ticketCount} is non-negative. Callers must run structural validation upstream — typically
 * via {@link TicketRequestValidator}.
 */
public interface TicketRequestAggregator {
  TicketTally aggregate(TicketRequest... ticketRequests);
}
