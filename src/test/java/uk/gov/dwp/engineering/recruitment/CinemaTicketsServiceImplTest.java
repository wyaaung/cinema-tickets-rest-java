package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.service.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.service.TicketRequestValidator;
import uk.gov.dwp.engineering.recruitment.service.TicketTally;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

@ExtendWith(MockitoExtension.class)
@DisplayName("CinemaTicketsServiceImpl — orchestrates validate → cost → pay → reserve")
class CinemaTicketsServiceImplTest {

  private static final Long ACCOUNT_ID = 42L;
  private static final String BOOKING_REFERENCE_PATTERN =
      "^BOOKING-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
          + "\\|account=\\d+\\|cost=\\S+\\|seats=\\d+$";

  @Mock
  private TicketRequestValidator validator;

  @Mock
  private TicketPriceCalculator priceCalculator;

  @Mock
  private PaymentService paymentService;

  @Mock
  private SeatReservationService seatReservationService;

  @InjectMocks
  private CinemaTicketsServiceImpl service;

  @Test
  @DisplayName("Valid purchase: validator → calculator → payment → reservation, in order")
  void givenValidPurchase_whenPurchaseTickets_thenPaymentAndReservationInvokedInOrder() {
    final TicketRequest adults = new TicketRequest(ADULT, 2);
    final TicketRequest children = new TicketRequest(CHILD, 3);
    final TicketRequest infants = new TicketRequest(INFANT, 1);
    final TicketTally tally = new TicketTally(2, 3, 1);
    final BigDecimal totalCost = BigDecimal.valueOf(95L);

    when(validator.validate(eq(ACCOUNT_ID), any(TicketRequest[].class))).thenReturn(tally);
    when(priceCalculator.totalCost(tally)).thenReturn(totalCost);

    final String confirmation = service.purchaseTickets(ACCOUNT_ID, adults, children, infants);

    final ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
    final ArgumentCaptor<Long> seatCaptor = ArgumentCaptor.forClass(Long.class);

    final InOrder inOrder =
        inOrder(validator, priceCalculator, paymentService, seatReservationService);
    inOrder.verify(validator).validate(eq(ACCOUNT_ID), any(TicketRequest[].class));
    inOrder.verify(priceCalculator).totalCost(tally);
    inOrder.verify(paymentService).debitAccount(eq(ACCOUNT_ID), amountCaptor.capture());
    inOrder.verify(seatReservationService).reserveSeats(eq(ACCOUNT_ID), seatCaptor.capture());

    assertEquals(0, totalCost.compareTo(amountCaptor.getValue()));
    assertEquals(Long.valueOf(tally.seatCount()), seatCaptor.getValue());
    assertTrue(confirmation.matches(BOOKING_REFERENCE_PATTERN),
        "expected confirmation to match pattern: " + confirmation);
    assertTrue(confirmation.contains("account=" + ACCOUNT_ID));
    assertTrue(confirmation.contains("cost=" + totalCost));
    assertTrue(confirmation.contains("seats=" + tally.seatCount()));
  }

  @Test
  @DisplayName("Adult-only purchase: seat count equals adult count")
  void givenAdultOnlyPurchase_whenPurchaseTickets_thenSeatCountEqualsAdultCount() {
    final TicketRequest adults = new TicketRequest(ADULT, 4);
    final TicketTally tally = new TicketTally(4, 0, 0);

    when(validator.validate(eq(ACCOUNT_ID), any(TicketRequest[].class))).thenReturn(tally);
    when(priceCalculator.totalCost(tally)).thenReturn(BigDecimal.valueOf(100L));

    service.purchaseTickets(ACCOUNT_ID, adults);

    final ArgumentCaptor<Long> seatCaptor = ArgumentCaptor.forClass(Long.class);
    verify(seatReservationService).reserveSeats(eq(ACCOUNT_ID), seatCaptor.capture());
    assertEquals(4L, seatCaptor.getValue());
  }

  @Test
  @DisplayName("Adult + infant purchase: infant excluded from seat count")
  void givenAdultAndInfantPurchase_whenPurchaseTickets_thenInfantExcludedFromSeatCount() {
    final TicketRequest adults = new TicketRequest(ADULT, 1);
    final TicketRequest infants = new TicketRequest(INFANT, 1);
    final TicketTally tally = new TicketTally(1, 0, 1);

    when(validator.validate(eq(ACCOUNT_ID), any(TicketRequest[].class))).thenReturn(tally);
    when(priceCalculator.totalCost(tally)).thenReturn(BigDecimal.valueOf(25L));

    service.purchaseTickets(ACCOUNT_ID, adults, infants);

    final ArgumentCaptor<Long> seatCaptor = ArgumentCaptor.forClass(Long.class);
    verify(seatReservationService).reserveSeats(eq(ACCOUNT_ID), seatCaptor.capture());
    assertEquals(1L, seatCaptor.getValue());
  }

  @Test
  @DisplayName("Validator throws: no payment, no reservation")
  void givenInvalidPurchase_whenPurchaseTickets_thenNoPaymentOrReservation() {
    when(validator.validate(eq(ACCOUNT_ID), any(TicketRequest[].class)))
        .thenThrow(new InvalidBookingException("invalid"));

    final TicketRequest request = new TicketRequest(CHILD, 1);

    final InvalidBookingException thrown = assertThrows(InvalidBookingException.class,
        () -> service.purchaseTickets(ACCOUNT_ID, request));
    assertEquals("invalid", thrown.getMessage());

    verifyNoInteractions(priceCalculator);
    verifyNoInteractions(paymentService);
    verifyNoInteractions(seatReservationService);
  }
}
