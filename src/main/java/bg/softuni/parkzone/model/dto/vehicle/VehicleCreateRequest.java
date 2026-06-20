package bg.softuni.parkzone.model.dto.vehicle;

import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleCreateRequest {

    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "^[A-Z]{2}\\d{4}[A-Z]{2}$", message = "Registration number must be in the format: AA1234BB")
    private String registrationNumber;

    @NotBlank(message = "Brand is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Brand must contain only letters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Model must contain only letters")
    private String model;

    @NotNull(message = "Vehicle type is required")
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @NotNull(message = "Engine type is required")
    @Enumerated(EnumType.STRING)
    private EngineType engineType;

    private boolean disabledParkingRequired;


}
