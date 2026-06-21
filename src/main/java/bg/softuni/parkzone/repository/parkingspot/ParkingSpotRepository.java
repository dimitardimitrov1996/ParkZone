package bg.softuni.parkzone.repository.parkingspot;

import bg.softuni.parkzone.model.entities.parkingsport.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID> {

    List<ParkingSpot> findAllByActiveTrue();

    List<ParkingSpot> findAllByParkingLotId(UUID parkingLotId);

    boolean existsByParkingLotIdAndSpotNumber(UUID parkingLotId, int spotNumber);

}
