package bg.softuni.parkzone.exception.vehicle;

import bg.softuni.parkzone.exception.ApplicationException;

import java.util.UUID;

public class VehicleNotFoundException extends ApplicationException {

    public VehicleNotFoundException(UUID id) {
        super(
                "Vehicle with id " + id + " was not found.",
                "404",
                "Vehicle not found"
        );
    }
}