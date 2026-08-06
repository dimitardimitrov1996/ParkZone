package bg.softuni.parkzone.exception.reservation;

import bg.softuni.parkzone.exception.ApplicationException;

import java.util.UUID;

public class ReservationNotFoundException extends ApplicationException {

    public ReservationNotFoundException(UUID id) {
        super("Reservation with id " + id + " was not found.", "404", "Reservation not found");
    }
}
