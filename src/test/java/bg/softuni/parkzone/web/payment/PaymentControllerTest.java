package bg.softuni.parkzone.web.payment;

import bg.softuni.parkzone.config.SecurityConfiguration;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.reservation.ReservationType;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.payment.PaymentService;
import bg.softuni.parkzone.service.user.AuthenticationUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(PaymentController.class)
@Import(SecurityConfiguration.class)
class PaymentControllerTest {

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    private UUID userId;
    private UUID reservationId;
    private UUID invoiceId;

    private AuthenticationUserDetails principal;
    private InvoiceResponse invoice;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        principal = AuthenticationUserDetails.builder()
                .id(userId)
                .username("user@test.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        invoice = InvoiceResponse.builder()
                .id(invoiceId)
                .reservationId(reservationId)
                .userId(userId)
                .amount(BigDecimal.valueOf(240))
                .currency("EUR")
                .status("PENDING")
                .createdOn(LocalDateTime.now())
                .build();

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber("CA1234AA")
                .brand("Tesla")
                .model("Model 3")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .build();

        ParkingLot parkingLot = ParkingLot.builder()
                .name("Indoor Parking")
                .build();

        ParkingSpot parkingSpot = ParkingSpot.builder()
                .spotNumber(8)
                .build();

        reservation = Reservation.builder()
                .id(reservationId)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .reservationType(ReservationType.MONTHLY)
                .status(ReservationStatus.PENDING_PAYMENT)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusMonths(1))
                .totalPrice(BigDecimal.valueOf(240))
                .build();
    }

    @Test
    void getPaymentPage_whenDataIsValid_shouldReturnPaymentView() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenReturn(invoice);
        when(paymentService.getReservationForPayment(reservationId, userId))
                .thenReturn(reservation);

        mockMvc.perform(get("/payments/reservation/{id}", reservationId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/pay"))
                .andExpect(model().attribute("reservationId", reservationId))
                .andExpect(model().attribute("invoice", invoice))
                .andExpect(model().attribute("reservation", reservation))
                .andExpect(model().attributeExists("paymentRequestDTO"));
    }

    @Test
    void getPaymentPage_whenServiceThrowsException_shouldRedirectToReservations() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenThrow(new IllegalArgumentException("Only pending invoices can be paid"));

        mockMvc.perform(get("/payments/reservation/{id}", reservationId)
                        .with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"))
                .andExpect(flash().attribute("errorMessage", "Only pending invoices can be paid"));
    }

    @Test
    void payReservationInvoice_whenDataIsValid_shouldPayAndRedirectToReservations() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenReturn(invoice);

        MockHttpServletRequestBuilder request = post("/payments/reservation/{id}", reservationId)
                .param("cardHolderName", "Ivan Ivanov")
                .param("cardNumber", "1234567812345678")
                .param("expirationDate", "12/30")
                .param("cvv", "123")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"))
                .andExpect(flash().attribute("successMessage", "Invoice paid successfully"));

        verify(paymentService).payReservationInvoice(eq(reservationId), eq(userId), any());
    }

    @Test
    void payReservationInvoice_whenInvoiceCannotBeLoaded_shouldRedirectToReservations() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenThrow(new IllegalArgumentException("Invoice not found"));

        MockHttpServletRequestBuilder request = post("/payments/reservation/{id}", reservationId)
                .param("cardHolderName", "Ivan Ivanov")
                .param("cardNumber", "1234567812345678")
                .param("expirationDate", "12/30")
                .param("cvv", "123")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"))
                .andExpect(flash().attribute("errorMessage", "Invoice not found"));

        verify(paymentService, never()).payReservationInvoice(any(), any(), any());
    }

    @Test
    void payReservationInvoice_whenValidationFails_shouldReturnPaymentView() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenReturn(invoice);
        when(paymentService.getReservationForPayment(reservationId, userId))
                .thenReturn(reservation);

        MockHttpServletRequestBuilder request = post("/payments/reservation/{id}", reservationId)
                .param("cardHolderName", "")
                .param("cardNumber", "abc")
                .param("expirationDate", "13/22")
                .param("cvv", "ab")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("payments/pay"))
                .andExpect(model().attribute("reservationId", reservationId))
                .andExpect(model().attribute("invoice", invoice))
                .andExpect(model().attribute("reservation", reservation))
                .andExpect(model().hasErrors());

        verify(paymentService, never()).payReservationInvoice(any(), any(), any());
    }

    @Test
    void payReservationInvoice_whenExpirationBusinessRuleFails_shouldReturnPaymentViewWithExpirationError() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenReturn(invoice);
        when(paymentService.getReservationForPayment(reservationId, userId))
                .thenReturn(reservation);

        doThrow(new IllegalArgumentException("Card expiration date must be in the future"))
                .when(paymentService)
                .payReservationInvoice(eq(reservationId), eq(userId), any());

        MockHttpServletRequestBuilder request = post("/payments/reservation/{id}", reservationId)
                .param("cardHolderName", "Ivan Ivanov")
                .param("cardNumber", "1234567812345678")
                .param("expirationDate", "01/25")
                .param("cvv", "123")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("payments/pay"))
                .andExpect(model().attribute("reservationId", reservationId))
                .andExpect(model().attribute("invoice", invoice))
                .andExpect(model().attribute("reservation", reservation))
                .andExpect(model().attributeHasFieldErrors("paymentRequestDTO", "expirationDate"));
    }

    @Test
    void payReservationInvoice_whenOtherBusinessRuleFails_shouldReturnPaymentViewWithGlobalError() throws Exception {
        when(paymentService.getInvoiceForReservationPayment(reservationId, userId))
                .thenReturn(invoice);
        when(paymentService.getReservationForPayment(reservationId, userId))
                .thenReturn(reservation);

        doThrow(new IllegalArgumentException("Only pending invoices can be paid"))
                .when(paymentService)
                .payReservationInvoice(eq(reservationId), eq(userId), any());

        MockHttpServletRequestBuilder request = post("/payments/reservation/{id}", reservationId)
                .param("cardHolderName", "Ivan Ivanov")
                .param("cardNumber", "1234567812345678")
                .param("expirationDate", "12/30")
                .param("cvv", "123")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("payments/pay"))
                .andExpect(model().attribute("reservationId", reservationId))
                .andExpect(model().attribute("invoice", invoice))
                .andExpect(model().attribute("reservation", reservation))
                .andExpect(model().hasErrors());
    }
}
