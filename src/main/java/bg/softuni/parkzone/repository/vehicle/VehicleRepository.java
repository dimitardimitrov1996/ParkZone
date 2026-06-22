package bg.softuni.parkzone.repository.vehicle;

import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByRegistrationNumber(String registrationNumber);

    List<Vehicle> findAllByOwnerIdAndActiveTrue(UUID ownerId);

    List<Vehicle> findAllByOrderByRegistrationNumberAsc();

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, UUID id);

}
