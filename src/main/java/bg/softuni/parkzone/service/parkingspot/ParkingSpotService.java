package bg.softuni.parkzone.service.parkingspot;

import bg.softuni.parkzone.model.entities.parkingsport.ParkingSpot;
import bg.softuni.parkzone.repository.parkingspot.ParkingSpotRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;

    public ParkingSpotService(ParkingSpotRepository parkingSpotRepository) {
        this.parkingSpotRepository = parkingSpotRepository;
    }

    public List<ParkingSpot> getAllActiveParkingSpots() {
        return parkingSpotRepository.findAllByActiveTrue()
                .stream()
                .sorted(
                        Comparator.comparing((ParkingSpot s) -> s.getParkingLot().getName())
                                .thenComparingInt(ParkingSpot::getSpotNumber)
                )
                .toList();
    }

}
