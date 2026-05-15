package uk.gov.dwp.engineering.recruitment.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

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
