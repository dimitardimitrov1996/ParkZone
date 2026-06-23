package bg.softuni.parkzone.repository.reservation;

import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findAllByUser(User user);

    List<Reservation> findAllByUserId(UUID userId);

    boolean existsByParkingSpotIdAndStatusAndStartDateBeforeAndEndDateAfter(
            UUID parkingSpotId,
            ReservationStatus status,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    boolean existsByVehicleIdAndStatusAndStartDateBeforeAndEndDateAfter(
            UUID vehicleId,
            ReservationStatus status,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    List<Reservation> findAllByUserIdAndStatus(UUID userId, ReservationStatus status);

    boolean existsByParkingSpotIdAndStatus(UUID parkingSpotId, ReservationStatus status);

    List<Reservation> findAllByVehicleIdAndStatus(UUID vehicleId, ReservationStatus status);

    boolean existsByParkingSpotIdAndStatusAndIdNotAndStartDateBeforeAndEndDateAfter(
            UUID parkingSpotId,
            ReservationStatus status,
            UUID reservationId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    boolean existsByVehicleIdAndStatusAndIdNotAndStartDateBeforeAndEndDateAfter(
            UUID vehicleId,
            ReservationStatus status,
            UUID reservationId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    List<Reservation> findAllByStatusAndEndDateBefore(ReservationStatus reservationStatus, LocalDateTime endDate);
}
