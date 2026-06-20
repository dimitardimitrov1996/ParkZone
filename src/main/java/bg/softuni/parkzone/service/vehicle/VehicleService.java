package bg.softuni.parkzone.service.vehicle;

import bg.softuni.parkzone.model.dto.vehicle.VehicleCreateRequest;
import bg.softuni.parkzone.model.dto.vehicle.VehicleDto;
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

    public void createVehicle(@Valid VehicleCreateRequest vehicleCreateRequest, UUID id) {

        if (vehicleRepository.existsByRegistrationNumber(
                vehicleCreateRequest.getRegistrationNumber())) {

            throw new IllegalArgumentException(
                    "Vehicle with this registration number already exists");
        }

        User owner = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(vehicleCreateRequest.getRegistrationNumber())
                .brand(vehicleCreateRequest.getBrand())
                .model(vehicleCreateRequest.getModel())
                .vehicleType(vehicleCreateRequest.getVehicleType())
                .engineType(vehicleCreateRequest.getEngineType())
                .disabledParkingRequired(vehicleCreateRequest.isDisabledParkingRequired())
                .owner(owner)
                .build();

        vehicleRepository.save(vehicle);

    }

    public List<Vehicle> getVehiclesByOwner(UUID ownerId) {
        return vehicleRepository.findAllByOwnerId(ownerId);
    }
}
