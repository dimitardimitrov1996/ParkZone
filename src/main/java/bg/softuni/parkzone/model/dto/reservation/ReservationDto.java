package bg.softuni.parkzone.model.dto.reservation;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.reservation.ReservationType;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReservationDto {

    private UUID id;
    private User user;
    private Vehicle vehicle;
    private ParkingLot parkingLot;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ReservationType reservationType;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdOn;

}
