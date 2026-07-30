package bg.softuni.parkzone.scheduler.reservation;

import bg.softuni.parkzone.service.reservation.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationScheduler {

    private final ReservationService reservationService;

    public ReservationScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void completeExpiredReservations() {
        reservationService.completeExpiredReservations();
    }

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredPendingPaymentReservations() {
        reservationService.cancelExpiredPendingPaymentReservations();
    }
}
