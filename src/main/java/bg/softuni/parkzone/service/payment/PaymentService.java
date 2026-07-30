package bg.softuni.parkzone.service.payment;


import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import bg.softuni.parkzone.model.dto.payment.PaymentRequestDTO;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.service.billing.client.BillingClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
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

        InvoiceResponse invoice = billingClient.getInvoiceByReservationId(reservation.getId());

        if (!"PENDING".equals(invoice.getStatus())) {
            throw new BusinessRuleException("Only pending invoices can be paid");
        }

        return invoice;
    }

    public void payReservationInvoice(UUID reservationId,
                                      UUID userId,
                                      PaymentRequestDTO paymentRequestDTO) {

        validateCardExpirationDate(paymentRequestDTO.getExpirationDate());

        Reservation reservation = getPayableReservation(reservationId, userId);

        InvoiceResponse invoice = billingClient.getInvoiceByReservationId(reservation.getId());

        if (!"PENDING".equals(invoice.getStatus())) {
            throw new BusinessRuleException("Only pending invoices can be paid");
        }

        billingClient.payInvoice(invoice.getId());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservationRepository.save(reservation);
    }

    private Reservation getPayableReservation(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot pay this reservation");
        }

        if (!canManageReservation(reservation)) {
            throw new BusinessRuleException("Only active reservations can be paid");
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

    private boolean canManageReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.ACTIVE
                || reservation.getStatus() == ReservationStatus.PENDING_PAYMENT;
    }

}
