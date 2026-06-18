package bg.softuni.parkzone.repository.reservation;

import bg.softuni.parkzone.model.entities.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
}
