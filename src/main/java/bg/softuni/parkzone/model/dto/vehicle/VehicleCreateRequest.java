package bg.softuni.parkzone.model.dto.vehicle;

import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleCreateRequest {

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Engine type is required")
    private EngineType engineType;

    private boolean disabledParkingRequired;


}
