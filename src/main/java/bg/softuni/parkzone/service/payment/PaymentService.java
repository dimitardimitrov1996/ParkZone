package bg.softuni.parkzone.service.payment;


import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.exception.billing.BillingServiceUnavailableException;
import bg.softuni.parkzone.exception.reservation.ReservationNotFoundException;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import bg.softuni.parkzone.model.dto.payment.PaymentRequestDTO;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.service.billing.client.BillingClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final ReservationRepository reservationRepository;

    private final BillingClient billingClient;

    public PaymentService(ReservationRepository reservationRepository,
                          BillingClient billingClient) {
        this.reservationRepository = reservationRepository;
        this.billingClient = billingClient;
    }

    public InvoiceResponse getInvoiceForReservationPayment(UUID reservationId, UUID userId) {

        Reservation reservation = getPayableReservation(reservationId, userId);

        InvoiceResponse invoice;

        try {
            invoice = billingClient.getInvoiceByReservationId(reservation.getId());
        } catch (FeignException e) {
            log.error("Billing service failed while loading invoice for reservation [{}]", reservation.getId(), e);
            throw new BillingServiceUnavailableException();
        }
            if (!"PENDING".equals(invoice.getStatus())) {
                throw new BusinessRuleException("Only pending invoices can be paid");
            }
            log.info("Invoice [{}] loaded for reservation payment [{}]", invoice.getId(), reservationId);
            return invoice;
    }

    public void payReservationInvoice(UUID reservationId,
                                      UUID userId,
                                      PaymentRequestDTO paymentRequestDTO) {

        validateCardExpirationDate(paymentRequestDTO.getExpirationDate());

        Reservation reservation = getPayableReservation(reservationId, userId);

        InvoiceResponse invoice;

        try {
            invoice = billingClient.getInvoiceByReservationId(reservation.getId());
        } catch (FeignException e) {
            log.error("Billing service failed while loading invoice for payment. Reservation [{}]", reservation.getId(), e);
            throw new BillingServiceUnavailableException();
        }

        if (!"PENDING".equals(invoice.getStatus())) {
            log.error("Invoice [{}] for reservation [{}] is not pending. Current status: {}", invoice.getId(), reservation.getId(), invoice.getStatus());
            throw new BusinessRuleException("Only pending invoices can be paid");
        }

        try {
            billingClient.payInvoice(invoice.getId());
        } catch (FeignException e) {
            log.error("Billing service failed while paying invoice [{}]", invoice.getId(), e);
            throw new BillingServiceUnavailableException();
        }

        reservation.setStatus(ReservationStatus.ACTIVE);
        reservationRepository.save(reservation);
        log.info("Invoice [{}] paid successfully. Reservation [{}] activated", invoice.getId(), reservationId);
    }

    private Reservation getPayableReservation(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot pay this reservation");
        }

        if (!canPayReservation(reservation)) {
            throw new BusinessRuleException("Only reservations with pending payment can be paid");
        }

        return reservation;
    }

    private void validateCardExpirationDate(String expirationDate) {

        if (expirationDate == null || !expirationDate.matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
            return;
        }

        int month = Integer.parseInt(expirationDate.substring(0, 2));
        int year = Integer.parseInt("20" + expirationDate.substring(3, 5));

        LocalDateTime now = LocalDateTime.now();

        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            throw new BusinessRuleException("Card expiration date must be in the future");
        }
    }

    public Reservation getReservationForPayment(UUID reservationId, UUID userId) {
        return getPayableReservation(reservationId, userId);
    }

    private boolean canPayReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.PENDING_PAYMENT;
    }

}
