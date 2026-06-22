package bg.softuni.parkzone.model.dto.reservation;

import bg.softuni.parkzone.model.entities.reservation.ReservationType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReservationEditRequestDTO {

    @NotNull(message = "Please select a vehicle")
    private UUID vehicleId;

    @NotNull(message = "Please select a parking lot")
    private UUID parkingLotId;

    @NotNull(message = "Please select a parking spot")
    private UUID parkingSpotId;

    @NotNull(message = "Please select reservation type")
    private ReservationType reservationType;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;
}
