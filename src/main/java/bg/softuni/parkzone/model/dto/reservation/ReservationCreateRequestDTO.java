package bg.softuni.parkzone.model.dto.reservation;


import bg.softuni.parkzone.model.entities.reservation.ReservationType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReservationCreateRequestDTO {

    @NotNull(message = "Please select a vehicle")
    private UUID vehicleId;

    @NotNull(message = "Please select a parking lot")
    private UUID parkingLotId;

    private boolean disabledParkingSpotRequired;

    private boolean electricChargingRequired;

    @NotNull(message = "Please select reservation type")
    private ReservationType reservationType;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;


}
