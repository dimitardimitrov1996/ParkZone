package bg.softuni.parkzone.repository.vehicle;

import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
}
