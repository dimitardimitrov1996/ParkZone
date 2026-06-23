package bg.softuni.parkzone.service.vehicle;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.dto.vehicle.VehicleCreateRequestDTO;
import bg.softuni.parkzone.model.dto.vehicle.VehicleEditDTO;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.repository.vehicle.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository, ReservationRepository reservationRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    public void createVehicle(@Valid VehicleCreateRequestDTO vehicleCreateRequestDTO, UUID id) {

        if (vehicleRepository.existsByRegistrationNumber(
                vehicleCreateRequestDTO.getRegistrationNumber())) {

            throw new BusinessRuleException(
                    "Vehicle with this registration number already exists");
        }

        User owner = userRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(vehicleCreateRequestDTO.getRegistrationNumber())
                .brand(vehicleCreateRequestDTO.getBrand())
                .model(vehicleCreateRequestDTO.getModel())
                .vehicleType(vehicleCreateRequestDTO.getVehicleType())
                .engineType(vehicleCreateRequestDTO.getEngineType())
                .disabledParkingRequired(vehicleCreateRequestDTO.isDisabledParkingRequired())
                .owner(owner)
                .active(true)
                .build();

        vehicleRepository.save(vehicle);

    }

    public List<Vehicle> getVehiclesByOwner(UUID ownerId) {
        return vehicleRepository.findAllByOwnerIdAndActiveTrue(ownerId);
    }

    public Vehicle findById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Vehicle not found"));
    }

    public void editVehicle(VehicleEditDTO request, UUID id) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Vehicle not found"));

        if (vehicleRepository.existsByRegistrationNumberAndIdNot(request.getRegistrationNumber(), id)) {
            throw new BusinessRuleException("Vehicle with this registration number already exists");
        }

        List<Reservation> activeReservations = reservationRepository
                .findAllByVehicleIdAndStatus(id, ReservationStatus.ACTIVE);

        boolean hasActiveChargingReservation = activeReservations.stream()
                .anyMatch(reservation -> reservation.getParkingSpot().isElectricChargingSpot());

        boolean hasActiveDisabledReservation = activeReservations.stream()
                .anyMatch(reservation -> reservation.getParkingSpot().isDisabledSpot());

        boolean hasActiveIndoorReservation = activeReservations.stream()
                .anyMatch(reservation -> reservation.getParkingLot().getParkingType() == ParkingType.INDOOR);

        if (hasActiveChargingReservation && request.getEngineType() != EngineType.ELECTRIC) {
            throw new BusinessRuleException(
                    "This vehicle has an active reservation for an electric charging spot"
            );
        }

        if (hasActiveDisabledReservation && !request.isDisabledParkingRequired()) {
            throw new BusinessRuleException(
                    "This vehicle has an active reservation for a disabled parking spot"
            );
        }

        if (hasActiveIndoorReservation && request.getVehicleType() == VehicleType.VAN) {
            throw new BusinessRuleException(
                    "This vehicle has an active indoor reservation and cannot be changed to VAN"
            );
        }

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setEngineType(request.getEngineType());
        vehicle.setDisabledParkingRequired(request.isDisabledParkingRequired());

        vehicleRepository.save(vehicle);
    }


    public VehicleEditDTO getVehicleForEdit(UUID id) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Vehicle not found"));

        return VehicleEditDTO.builder()
                .registrationNumber(vehicle.getRegistrationNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .vehicleType(vehicle.getVehicleType())
                .engineType(vehicle.getEngineType())
                .disabledParkingRequired(vehicle.isDisabledParkingRequired())
                .build();
    }

    public void deleteVehicle(UUID vehicleId, UUID userId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessRuleException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot delete this vehicle");
        }

        if (!vehicle.isActive()) {
            throw new BusinessRuleException("Vehicle is already deleted");
        }

        List<Reservation> activeReservations = reservationRepository
                .findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE);

        for (Reservation reservation : activeReservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
        }

        reservationRepository.saveAll(activeReservations);

        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAllByOrderByRegistrationNumberAsc();
    }

    public void deleteVehicleByAdmin(UUID vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessRuleException("Vehicle not found"));

        if (!vehicle.isActive()) {
            throw new BusinessRuleException("Vehicle is already deleted");
        }

        List<Reservation> activeReservations = reservationRepository
                .findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE);

        for (Reservation reservation : activeReservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
        }

        reservationRepository.saveAll(activeReservations);

        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }

    public void activateVehicleByAdmin(UUID vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessRuleException("Vehicle not found"));

        if (vehicle.isActive()) {
            throw new BusinessRuleException("Vehicle is already active");
        }

        if (!vehicle.getOwner().isActive()) {
            throw new BusinessRuleException("Cannot activate vehicle because the owner is inactive");
        }

        vehicle.setActive(true);
        vehicleRepository.save(vehicle);
    }

}
