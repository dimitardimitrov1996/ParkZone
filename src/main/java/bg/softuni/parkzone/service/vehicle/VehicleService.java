package bg.softuni.parkzone.service.vehicle;

import bg.softuni.parkzone.model.dto.vehicle.VehicleCreateRequestDTO;
import bg.softuni.parkzone.model.dto.vehicle.VehicleEditDTO;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
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

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    public void createVehicle(@Valid VehicleCreateRequestDTO vehicleCreateRequestDTO, UUID id) {

        if (vehicleRepository.existsByRegistrationNumber(
                vehicleCreateRequestDTO.getRegistrationNumber())) {

            throw new IllegalArgumentException(
                    "Vehicle with this registration number already exists");
        }

        User owner = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(vehicleCreateRequestDTO.getRegistrationNumber())
                .brand(vehicleCreateRequestDTO.getBrand())
                .model(vehicleCreateRequestDTO.getModel())
                .vehicleType(vehicleCreateRequestDTO.getVehicleType())
                .engineType(vehicleCreateRequestDTO.getEngineType())
                .disabledParkingRequired(vehicleCreateRequestDTO.isDisabledParkingRequired())
                .owner(owner)
                .build();

        vehicleRepository.save(vehicle);

    }

    public List<Vehicle> getVehiclesByOwner(UUID ownerId) {
        return vehicleRepository.findAllByOwnerId(ownerId);
    }

    public Vehicle findById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
    }

    public void editVehicle(VehicleEditDTO request, UUID id) {

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

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
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        return VehicleEditDTO.builder()
                .registrationNumber(vehicle.getRegistrationNumber())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .vehicleType(vehicle.getVehicleType())
                .engineType(vehicle.getEngineType())
                .disabledParkingRequired(vehicle.isDisabledParkingRequired())
                .build();
    }

    public void deleteVehicle(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        vehicleRepository.delete(vehicle);

    }
}
