package bg.softuni.parkzone.service.parkingspot;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.repository.parkinglot.ParkingLotRepository;
import bg.softuni.parkzone.repository.parkingspot.ParkingSpotRepository;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ReservationRepository reservationRepository;

    public ParkingSpotService(ParkingSpotRepository parkingSpotRepository, ParkingLotRepository parkingLotRepository, ReservationRepository reservationRepository) {
        this.parkingSpotRepository = parkingSpotRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.reservationRepository = reservationRepository;
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
    public UUID makeDisabledSpot(UUID parkingSpotId) {
        ParkingSpot parkingSpot = getParkingSpot(parkingSpotId);
        validateSpotCanBeChanged(parkingSpot);

        parkingSpot.setDisabledSpot(true);
        parkingSpot.setElectricChargingSpot(false);

        parkingSpotRepository.save(parkingSpot);
        refreshParkingLotCounters(parkingSpot.getParkingLot().getId());

        return parkingSpot.getParkingLot().getId();
    }

    public UUID makeElectricChargingSpot(UUID parkingSpotId) {
        ParkingSpot parkingSpot = getParkingSpot(parkingSpotId);
        validateSpotCanBeChanged(parkingSpot);

        parkingSpot.setElectricChargingSpot(true);
        parkingSpot.setDisabledSpot(false);

        parkingSpotRepository.save(parkingSpot);
        refreshParkingLotCounters(parkingSpot.getParkingLot().getId());

        return parkingSpot.getParkingLot().getId();
    }

    public UUID makeNormalSpot(UUID parkingSpotId) {
        ParkingSpot parkingSpot = getParkingSpot(parkingSpotId);
        validateSpotCanBeChanged(parkingSpot);

        parkingSpot.setDisabledSpot(false);
        parkingSpot.setElectricChargingSpot(false);

        parkingSpotRepository.save(parkingSpot);
        refreshParkingLotCounters(parkingSpot.getParkingLot().getId());

        return parkingSpot.getParkingLot().getId();
    }

    public UUID toggleActive(UUID parkingSpotId) {
        ParkingSpot parkingSpot = getParkingSpot(parkingSpotId);

        if (parkingSpot.isActive()) {
            validateSpotCanBeChanged(parkingSpot);
        }

        parkingSpot.setActive(!parkingSpot.isActive());

        parkingSpotRepository.save(parkingSpot);
        refreshParkingLotCounters(parkingSpot.getParkingLot().getId());

        return parkingSpot.getParkingLot().getId();
    }

    private ParkingSpot getParkingSpot(UUID parkingSpotId) {
        return parkingSpotRepository.findById(parkingSpotId)
                .orElseThrow(() -> new BusinessRuleException("Parking spot not found"));
    }

    private void validateSpotCanBeChanged(ParkingSpot parkingSpot) {
        boolean hasActiveReservation = reservationRepository
                .existsByParkingSpotIdAndStatus(parkingSpot.getId(), ReservationStatus.ACTIVE);

        if (hasActiveReservation) {
            throw new BusinessRuleException("This parking spot has an active reservation and cannot be changed");
        }
    }

    private void refreshParkingLotCounters(UUID parkingLotId) {
        ParkingLot parkingLot = parkingLotRepository.findById(parkingLotId)
                .orElseThrow(() -> new BusinessRuleException("Parking lot not found"));

        int disabledSpots = parkingSpotRepository.countByParkingLotIdAndDisabledSpotTrue(parkingLotId);
        int electricSpots = parkingSpotRepository.countByParkingLotIdAndElectricChargingSpotTrue(parkingLotId);

        parkingLot.setDisabledParkingSpots(disabledSpots);
        parkingLot.setElectricChargingSpots(electricSpots);

        parkingLotRepository.save(parkingLot);
    }


    public List<ParkingSpot> getSpotsByParkingLot(UUID id) {
        return parkingSpotRepository.findAllByParkingLotIdOrderBySpotNumberAsc(id);
    }
}
