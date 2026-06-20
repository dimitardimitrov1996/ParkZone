package bg.softuni.parkzone.config;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.repository.parkinglot.ParkingLotRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ParkingLotRepository parkingLotRepository;

    public DataInitializer(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    @Override
    public void run(String... args) {

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
