package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.service.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.service.TicketRequestValidator;
import uk.gov.dwp.engineering.recruitment.service.TicketTally;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

/**
 * Orchestrates a ticket purchase: validates the request, prices it, takes payment, then reserves
 * seats. Each step is delegated to a collaborator behind an interface so this class only owns the
 * sequence and the booking-reference format.
 *
 * <p>On success a booking reference of the form
 * {@code BOOKING-<uuid>|account=<id>|cost=<total>|seats=<count>} is returned. The format is
 * deliberately parseable and log-friendly; the UUID portion is unique per call.
 *
 * <p>On any validation failure an {@link InvalidBookingException} is thrown <em>before</em>
 * payment or reservation occur — no partial side effects.
 */
@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final TicketRequestValidator validator;
  private final TicketPriceCalculator priceCalculator;
  private final PaymentService paymentService;
  private final SeatReservationService seatReservationService;

  public CinemaTicketsServiceImpl(final TicketRequestValidator validator,
      final TicketPriceCalculator priceCalculator, final PaymentService paymentService,
      final SeatReservationService seatReservationService) {
    this.validator = Objects.requireNonNull(validator, "validator");
    this.priceCalculator = Objects.requireNonNull(priceCalculator, "priceCalculator");
    this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
    this.seatReservationService =
        Objects.requireNonNull(seatReservationService, "seatReservationService");
  }

  @Override
  public String purchaseTickets(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    final TicketTally tally = validator.validate(accountId, ticketRequests);
    final BigDecimal totalCost = priceCalculator.totalCost(tally);

    paymentService.debitAccount(accountId, totalCost);
    seatReservationService.reserveSeats(accountId, (long) tally.seatCount());

    return buildBookingReference(accountId, totalCost, tally.seatCount());
  }

  private static String buildBookingReference(final Long accountId, final BigDecimal totalCost,
      final int seatCount) {
    return "BOOKING-" + UUID.randomUUID() + "|account=" + accountId + "|cost="
        + totalCost.toPlainString() + "|seats=" + seatCount;
  }
}
