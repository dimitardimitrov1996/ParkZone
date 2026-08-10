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

    boolean existsByParkingSpotIdAndStatusInAndStartDateBeforeAndEndDateAfter(
            UUID parkingSpotId,
            List<ReservationStatus> statuses,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    boolean existsByVehicleIdAndStatusInAndStartDateBeforeAndEndDateAfter(
            UUID vehicleId,
            List<ReservationStatus> statuses,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    List<Reservation> findAllByUserIdAndStatusIn(
            UUID userId,
            List<ReservationStatus> statuses
    );

    boolean existsByParkingSpotIdAndStatusIn(
            UUID parkingSpotId,
            List<ReservationStatus> statuses
    );

    List<Reservation> findAllByVehicleIdAndStatusIn(
            UUID vehicleId,
            List<ReservationStatus> statuses
    );


    boolean existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
            UUID parkingSpotId,
            List<ReservationStatus> statuses,
            UUID reservationId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    boolean existsByVehicleIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
            UUID vehicleId,
            List<ReservationStatus> statuses,
            UUID reservationId,
            LocalDateTime endDate,
            LocalDateTime startDate
    );

    List<Reservation> findAllByStatusAndEndDateBefore(ReservationStatus reservationStatus, LocalDateTime endDate);

    List<Reservation> findAllByStatusAndStartDateBefore(
            ReservationStatus reservationStatus,
            LocalDateTime startDate
    );


}
