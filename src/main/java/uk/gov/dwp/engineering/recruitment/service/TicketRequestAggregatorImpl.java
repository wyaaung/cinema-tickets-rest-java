package uk.gov.dwp.engineering.recruitment.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

/**
 * Aggregates a {@link TicketRequest} array into a {@link TicketTally} by grouping per
 * {@link TicketType} and summing {@code ticketCount}. Assumes the array and its elements are
 * non-null and that each {@code ticketCount} is non-negative — callers must run structural
 * validation upstream (see {@link TicketRequestValidator}). The internal {@link EnumMap}
 * keeps the memory footprint proportional to the number of {@code TicketType} values, not
 * the size of the input.
 */
@Service
public class TicketRequestAggregatorImpl implements TicketRequestAggregator {

  @Override
  public TicketTally aggregate(final TicketRequest... ticketRequests) {
    final Map<TicketType, Integer> counts = Arrays.stream(ticketRequests)
        .collect(Collectors.groupingBy(
            TicketRequest::type,
            () -> new EnumMap<TicketType, Integer>(TicketType.class),
            Collectors.summingInt(TicketRequest::ticketCount)));

    return new TicketTally(
        counts.getOrDefault(TicketType.ADULT, 0),
        counts.getOrDefault(TicketType.CHILD, 0),
        counts.getOrDefault(TicketType.INFANT, 0)
    );
  }
}
