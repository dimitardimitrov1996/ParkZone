package bg.softuni.parkzone.config;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.parkingsport.ParkingSpot;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.repository.parkinglot.ParkingLotRepository;
import bg.softuni.parkzone.repository.parkingspot.ParkingSpotRepository;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.repository.vehicle.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ParkingLotRepository parkingLotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ParkingSpotRepository parkingSpotRepository;
    private final VehicleRepository vehicleRepository;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.user.password}")
    private String userPassword;

    public DataInitializer(ParkingLotRepository parkingLotRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, ParkingSpotRepository parkingSpotRepository, VehicleRepository vehicleRepository) {
        this.parkingLotRepository = parkingLotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.parkingSpotRepository = parkingSpotRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public void run(String... args) {

        initAdmin();
        initDefaultUser();
        initParkingLots();
        initParkingSpots();
        initDefaultVehicles();
    }

    private void initDefaultUser() {

        if (userRepository.existsByEmail("user@abv.bg")) {
            return;
        }

        User user = User.builder()
                .username("user1")
                .email("user@abv.bg")
                .password(passwordEncoder.encode(userPassword))
                .firstName("Basic")
                .lastName("User")
                .phoneNumber("0888888888")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        userRepository.save(user);
    }

    private void initAdmin() {

        if (userRepository.existsByEmail("admin@abv.bg")) {
            return;
        }

        User admin = User.builder()
                .username("admin1")
                .email("admin@abv.bg")
                .password(passwordEncoder.encode(adminPassword))
                .firstName("Admin")
                .lastName("User")
                .phoneNumber("0899999999")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
    }

    private void initParkingLots() {

        if (parkingLotRepository.findByParkingType(ParkingType.OUTDOOR).isEmpty()) {
            ParkingLot outdoorParking = ParkingLot.builder()
                    .name("Outdoor Parking")
                    .parkingType(ParkingType.OUTDOOR)
                    .capacity(30)
                    .disabledParkingSpots(5)
                    .electricChargingSpots(0)
                    .dailyPrice(BigDecimal.valueOf(5))
                    .monthlyPrice(BigDecimal.valueOf(120))
                    .yearlyPrice(BigDecimal.valueOf(1200))
                    .build();

            parkingLotRepository.save(outdoorParking);
        }

        if (parkingLotRepository.findByParkingType(ParkingType.INDOOR).isEmpty()) {
            ParkingLot indoorParking = ParkingLot.builder()
                    .name("Indoor Parking")
                    .parkingType(ParkingType.INDOOR)
                    .capacity(30)
                    .disabledParkingSpots(5)
                    .electricChargingSpots(5)
                    .dailyPrice(BigDecimal.valueOf(10))
                    .monthlyPrice(BigDecimal.valueOf(240))
                    .yearlyPrice(BigDecimal.valueOf(2400))
                    .build();

            parkingLotRepository.save(indoorParking);
        }
    }

    private void initParkingSpots() {

        ParkingLot outdoorParking = parkingLotRepository.findByParkingType(ParkingType.OUTDOOR)
                .orElseThrow(() -> new IllegalArgumentException("Outdoor parking not found"));

        ParkingLot indoorParking = parkingLotRepository.findByParkingType(ParkingType.INDOOR)
                .orElseThrow(() -> new IllegalArgumentException("Indoor parking not found"));

        createParkingSpots(outdoorParking);
        createParkingSpots(indoorParking);
    }

    private void createParkingSpots(ParkingLot parkingLot) {

        for (int spotNumber = 1; spotNumber <= parkingLot.getCapacity(); spotNumber++) {

            boolean spotAlreadyExists = parkingSpotRepository
                    .existsByParkingLotIdAndSpotNumber(parkingLot.getId(), spotNumber);

            if (spotAlreadyExists) {
                continue;
            }

            boolean disabledSpot = spotNumber <= parkingLot.getDisabledParkingSpots();

            boolean electricChargingSpot =
                    spotNumber > parkingLot.getDisabledParkingSpots()
                            && spotNumber <= parkingLot.getDisabledParkingSpots()
                            + parkingLot.getElectricChargingSpots();

            ParkingSpot parkingSpot = ParkingSpot.builder()
                    .spotNumber(spotNumber)
                    .disabledSpot(disabledSpot)
                    .electricChargingSpot(electricChargingSpot)
                    .active(true)
                    .parkingLot(parkingLot)
                    .build();

            parkingSpotRepository.save(parkingSpot);
        }
    }

    private void initDefaultVehicles() {

        User defaultUser = userRepository.findByEmail("user@abv.bg")
                .orElseThrow(() -> new IllegalArgumentException("Default user not found"));

        createVehicleIfNotExists(
                "CA1234AA",
                "Volkswagen",
                "Golf",
                VehicleType.CAR,
                EngineType.DIESEL,
                false,
                defaultUser
        );

        createVehicleIfNotExists(
                "CA2222EV",
                "Tesla",
                "Model 3",
                VehicleType.CAR,
                EngineType.ELECTRIC,
                false,
                defaultUser
        );

        createVehicleIfNotExists(
                "CA3333BB",
                "Mercedes",
                "Sprinter",
                VehicleType.VAN,
                EngineType.DIESEL,
                false,
                defaultUser
        );

        createVehicleIfNotExists(
                "CA4444DD",
                "Toyota",
                "Corolla",
                VehicleType.CAR,
                EngineType.PETROL,
                true,
                defaultUser
        );
    }

    private void createVehicleIfNotExists(String registrationNumber,
                                          String brand,
                                          String model,
                                          VehicleType vehicleType,
                                          EngineType engineType,
                                          boolean disabledParkingRequired,
                                          User owner) {

        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
            return;
        }

        Vehicle vehicle = Vehicle.builder()
                .registrationNumber(registrationNumber)
                .brand(brand)
                .model(model)
                .vehicleType(vehicleType)
                .engineType(engineType)
                .disabledParkingRequired(disabledParkingRequired)
                .owner(owner)
                .build();

        vehicleRepository.save(vehicle);
    }

}
