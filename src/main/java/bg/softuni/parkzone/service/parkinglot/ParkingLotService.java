package bg.softuni.parkzone.service.parkinglot;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.repository.parkinglot.ParkingLotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    public ParkingLotService(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }


    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    public ParkingLot getById(UUID id) {
        return parkingLotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parking lot not found"));
    }

}

