package bg.softuni.parkzone.model.entities.reservation;

import java.util.List;

public final class ReservationStatuses {

    private ReservationStatuses() {
    }

    public static final List<ReservationStatus> BLOCKING = List.of(
            ReservationStatus.ACTIVE,
            ReservationStatus.PENDING_PAYMENT
    );
}
