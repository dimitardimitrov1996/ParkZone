package bg.softuni.parkzone.repository.parkinglot;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {
}
