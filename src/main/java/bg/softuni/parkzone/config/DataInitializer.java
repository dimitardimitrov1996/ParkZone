package bg.softuni.parkzone.config;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.repository.parkinglot.ParkingLotRepository;
import bg.softuni.parkzone.repository.user.UserRepository;
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

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.user.password}")
    private String userPassword;

    public DataInitializer(ParkingLotRepository parkingLotRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.parkingLotRepository = parkingLotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        initAdmin();
        initDefaultUser();
        initParkingLots();
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
                .phoneNumber("0888123456")
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
                    .capacity(100)
                    .disabledParkingSpots(10)
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
                    .capacity(100)
                    .disabledParkingSpots(10)
                    .electricChargingSpots(10)
                    .dailyPrice(BigDecimal.valueOf(10))
                    .monthlyPrice(BigDecimal.valueOf(240))
                    .yearlyPrice(BigDecimal.valueOf(2400))
                    .build();

            parkingLotRepository.save(indoorParking);
        }
    }



}
