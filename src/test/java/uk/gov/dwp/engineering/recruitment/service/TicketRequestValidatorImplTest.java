package uk.gov.dwp.engineering.recruitment.service;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_EXCEEDS_MAX;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_INVALID_ACCOUNT_ID;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_NEGATIVE_COUNT;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_NO_ADULT;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_NO_TICKETS;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_NULL_ELEMENT;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_NULL_TYPE;
import static uk.gov.dwp.engineering.recruitment.service.TicketRequestValidatorImpl.MSG_REQUESTS_EMPTY;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketRequestValidatorImpl — applies booking rules and returns the aggregated tally")
class TicketRequestValidatorImplTest {

  private static final Long VALID_ACCOUNT_ID = 123L;
  private static final TicketRequest VALID_ADULT_REQUEST = new TicketRequest(ADULT, 1);

  @Mock
  private TicketRequestAggregator aggregator;

  @InjectMocks
  private TicketRequestValidatorImpl validator;

  // ---- Pre-aggregation rejection matrix ----

  @ParameterizedTest(name = "[{index}] {0} → InvalidBookingException with reason {3} and expected message; aggregator never called")
  @MethodSource("preAggregationRejections")
  @DisplayName("Pre-aggregation rejections short-circuit before the aggregator runs")
  void preAggregationRejection_throwsAndSkipsAggregator(
      final String label,
      final Long accountId,
      final TicketRequest[] requests,
      final String expectedMessage) {

    final InvalidBookingException thrown = assertThrows(InvalidBookingException.class,
        () -> validator.validate(accountId, requests));

    assertEquals(expectedMessage, thrown.getMessage());
    verifyNoInteractions(aggregator);
  }

  static Stream<Arguments> preAggregationRejections() {
    return Stream.of(
        Arguments.of("null accountId",
            null, new TicketRequest[]{VALID_ADULT_REQUEST},
            MSG_INVALID_ACCOUNT_ID),
        Arguments.of("zero accountId",
            0L, new TicketRequest[]{VALID_ADULT_REQUEST},
            MSG_INVALID_ACCOUNT_ID),
        Arguments.of("negative accountId",
            -1L, new TicketRequest[]{VALID_ADULT_REQUEST},
            MSG_INVALID_ACCOUNT_ID),
        Arguments.of("null ticketRequests array",
            VALID_ACCOUNT_ID, null,
            MSG_REQUESTS_EMPTY),
        Arguments.of("empty ticketRequests array",
            VALID_ACCOUNT_ID, new TicketRequest[0],
            MSG_REQUESTS_EMPTY),
        Arguments.of("array contains null element",
            VALID_ACCOUNT_ID, new TicketRequest[]{VALID_ADULT_REQUEST, null},
            MSG_NULL_ELEMENT),
        Arguments.of("element with null type",
            VALID_ACCOUNT_ID, new TicketRequest[]{new TicketRequest(null, 1)},
            MSG_NULL_TYPE),
        Arguments.of("element with negative count",
            VALID_ACCOUNT_ID, new TicketRequest[]{new TicketRequest(ADULT, -1)},
            MSG_NEGATIVE_COUNT)
    );
  }

  // ---- Post-aggregation rejection matrix ----

  @ParameterizedTest(name = "[{index}] {0} → InvalidBookingException with reason {2} and expected message")
  @MethodSource("postAggregationRejections")
  @DisplayName("Post-aggregation rejections check the tally returned by the aggregator")
  void postAggregationRejection_throws(
      final String label,
      final TicketRequest[] requests,
      final TicketTally tallyFromAggregator,
      final String expectedMessage) {

    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(tallyFromAggregator);

    final InvalidBookingException thrown = assertThrows(InvalidBookingException.class,
        () -> validator.validate(VALID_ACCOUNT_ID, requests));

    assertEquals(expectedMessage, thrown.getMessage());
  }

  static Stream<Arguments> postAggregationRejections() {
    return Stream.of(
        Arguments.of("aggregate total exceeds max (26)",
            new TicketRequest[]{new TicketRequest(ADULT, 26)},
            new TicketTally(26, 0, 0),
            MSG_EXCEEDS_MAX),
        Arguments.of("aggregate total is zero",
            new TicketRequest[]{new TicketRequest(ADULT, 0)},
            new TicketTally(0, 0, 0),
            MSG_NO_TICKETS),
        Arguments.of("child only, no adult",
            new TicketRequest[]{new TicketRequest(CHILD, 1)},
            new TicketTally(0, 1, 0),
            MSG_NO_ADULT),
        Arguments.of("infant only, no adult",
            new TicketRequest[]{new TicketRequest(INFANT, 1)},
            new TicketTally(0, 0, 1),
            MSG_NO_ADULT),
        Arguments.of("child and infant, no adult",
            new TicketRequest[]{new TicketRequest(CHILD, 1), new TicketRequest(INFANT, 1)},
            new TicketTally(0, 1, 1),
            MSG_NO_ADULT)
    );
  }

  // ---- Success paths ----

  @Test
  @DisplayName("Adult-only request returns the adult-only tally")
  void givenValidAdultOnlyRequest_whenValidate_thenTallyReturned() {
    final TicketRequest request = new TicketRequest(ADULT, 1);
    final TicketTally expected = new TicketTally(1, 0, 0);
    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(expected);

    final TicketTally actual = validator.validate(VALID_ACCOUNT_ID, request);

    assertEquals(expected, actual);
    verify(aggregator).aggregate(request);
  }

  @Test
  @DisplayName("Adult, child, and infant request returns the mixed-type tally")
  void givenValidAdultChildInfantRequest_whenValidate_thenTallyReturned() {
    final TicketRequest[] requests = new TicketRequest[]{
        new TicketRequest(ADULT, 1),
        new TicketRequest(CHILD, 1),
        new TicketRequest(INFANT, 1)
    };
    final TicketTally expected = new TicketTally(1, 1, 1);
    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(expected);

    final TicketTally actual = validator.validate(VALID_ACCOUNT_ID, requests);

    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("More infants than adults is allowed (lap rule is not 1:1)")
  void givenMoreInfantsThanAdults_whenValidate_thenLapRuleNotEnforcedAndTallyReturned() {
    final TicketRequest[] requests = new TicketRequest[]{
        new TicketRequest(ADULT, 1),
        new TicketRequest(INFANT, 2)
    };
    final TicketTally expected = new TicketTally(1, 0, 2);
    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(expected);

    final TicketTally actual = validator.validate(VALID_ACCOUNT_ID, requests);

    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("One below maximum (24) is accepted")
  void givenOneBelowMaximumTotal_whenValidate_thenTallyReturned() {
    final TicketRequest[] requests = new TicketRequest[]{
        new TicketRequest(ADULT, 12),
        new TicketRequest(CHILD, 12)
    };
    final TicketTally expected = new TicketTally(12, 12, 0);
    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(expected);

    final TicketTally actual = validator.validate(VALID_ACCOUNT_ID, requests);

    assertEquals(expected, actual);
    assertEquals(24, actual.total());
  }

  @Test
  @DisplayName("Maximum allowed total (25) is accepted")
  void givenMaximumAllowedTotal_whenValidate_thenTallyReturned() {
    final TicketRequest[] requests = new TicketRequest[]{
        new TicketRequest(ADULT, 13),
        new TicketRequest(CHILD, 12)
    };
    final TicketTally expected = new TicketTally(13, 12, 0);
    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(expected);

    final TicketTally actual = validator.validate(VALID_ACCOUNT_ID, requests);

    assertEquals(expected, actual);
    assertEquals(25, actual.total());
  }

  @Test
  @DisplayName("Zero-count entry per request is allowed (rejection only at aggregate)")
  void givenRequestWithZeroCountElement_whenValidate_thenZeroPerRequestAllowed() {
    final TicketRequest[] requests = new TicketRequest[]{
        new TicketRequest(ADULT, 1),
        new TicketRequest(CHILD, 0)
    };
    final TicketTally expected = new TicketTally(1, 0, 0);
    when(aggregator.aggregate(any(TicketRequest[].class))).thenReturn(expected);

    final TicketTally actual = validator.validate(VALID_ACCOUNT_ID, requests);

    assertEquals(expected, actual);
  }
}
