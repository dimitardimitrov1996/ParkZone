package bg.softuni.parkzone.model.dto.vehicle;

import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VehicleDTO {

    private UUID id;
    private String registrationNumber;
    private String brand;
    private String model;
    private VehicleType vehicleType;
    private EngineType engineType;
    private boolean disabledParkingRequired;
    private User owner;

}
