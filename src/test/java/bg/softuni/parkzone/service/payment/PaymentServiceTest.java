package bg.softuni.parkzone.service.payment;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.exception.billing.BillingServiceUnavailableException;
import bg.softuni.parkzone.exception.reservation.ReservationNotFoundException;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import bg.softuni.parkzone.model.dto.payment.PaymentRequestDTO;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.service.billing.client.BillingClient;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PaymentServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BillingClient billingClient;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userId;
    private UUID reservationId;
    private UUID invoiceId;
    private PaymentRequestDTO validPaymentRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        validPaymentRequest = PaymentRequestDTO.builder()
                .cardHolderName("Dimitar Dimitrov")
                .cardNumber("1234567812345678")
                .expirationDate("12/30")
                .cvv("123")
                .build();
    }

    @Test
    void payReservationInvoice_whenReservationIsPendingPayment_shouldPayInvoiceAndActivateReservation() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        InvoiceResponse invoice = createInvoice("PENDING");

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(billingClient.getInvoiceByReservationId(reservationId)).thenReturn(invoice);

        paymentService.payReservationInvoice(reservationId, userId, validPaymentRequest);

        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
        verify(billingClient).payInvoice(invoiceId);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void payReservationInvoice_whenReservationIsActive_shouldThrowBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> paymentService.payReservationInvoice(reservationId, userId, createValidPaymentRequest()));

        verify(billingClient, never()).payInvoice(any());
    }

    @Test
    void payReservationInvoice_whenUserIsNotOwner_shouldThrowBusinessRuleException() {

        UUID otherUserId = UUID.randomUUID();

        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> paymentService.payReservationInvoice(reservationId, otherUserId, createValidPaymentRequest()));

        verify(billingClient, never()).payInvoice(any());
    }

    @Test
    void payReservationInvoice_whenCardIsExpired_shouldThrowBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        PaymentRequestDTO request = createValidPaymentRequest();
        request.setExpirationDate("01/20");

        assertThrows(BusinessRuleException.class,
                () -> paymentService.payReservationInvoice(reservationId, userId, request));

        verifyNoInteractions(reservationRepository);
        verifyNoInteractions(billingClient);
    }

    @Test
    void payReservationInvoice_whenBillingServiceFails_shouldThrowBillingServiceUnavailableException() {

        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(billingClient.getInvoiceByReservationId(reservationId)).thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> paymentService.payReservationInvoice(reservationId, userId, createValidPaymentRequest()));
    }

    @Test
    void getInvoiceForReservationPayment_whenInvoiceIsPending_shouldReturnInvoice() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        InvoiceResponse invoice = createInvoice("PENDING");

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(billingClient.getInvoiceByReservationId(reservationId)).thenReturn(invoice);

        InvoiceResponse result = paymentService.getInvoiceForReservationPayment(reservationId, userId);

        assertEquals(invoice, result);
    }

    @Test
    void getInvoiceForReservationPayment_whenBillingServiceFails_shouldThrowBillingServiceUnavailableException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(billingClient.getInvoiceByReservationId(reservationId)).thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> paymentService.getInvoiceForReservationPayment(reservationId, userId));
    }

    @Test
    void getInvoiceForReservationPayment_whenInvoiceIsNotPending_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        InvoiceResponse invoice = createInvoice("PAID");

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(billingClient.getInvoiceByReservationId(reservationId)).thenReturn(invoice);

        assertThrows(BusinessRuleException.class,
                () -> paymentService.getInvoiceForReservationPayment(reservationId, userId));
    }

    @Test
    void getReservationForPayment_whenReservationIsPendingPayment_shouldReturnReservation() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        Reservation result = paymentService.getReservationForPayment(reservationId, userId);

        assertEquals(reservation, result);
    }

    @Test
    void payReservationInvoice_whenReservationDoesNotExist_shouldThrowReservationNotFoundException() {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> paymentService.payReservationInvoice(reservationId, userId, validPaymentRequest));

        verifyNoInteractions(billingClient);
    }

    private Reservation createReservation(ReservationStatus status) {
        User user = User.builder()
                .id(userId)
                .build();

        return Reservation.builder()
                .id(reservationId)
                .user(user)
                .status(status)
                .build();
    }

    private InvoiceResponse createInvoice(String status) {
        return InvoiceResponse.builder()
                .id(invoiceId)
                .reservationId(reservationId)
                .userId(userId)
                .amount(BigDecimal.valueOf(100))
                .currency("EUR")
                .status(status)
                .build();
    }

    private PaymentRequestDTO createValidPaymentRequest() {
        return PaymentRequestDTO.builder()
                .cardHolderName("Dimitar Dimitrov")
                .cardNumber("1234567812345678")
                .expirationDate("12/30")
                .cvv("123")
                .build();
    }
}
