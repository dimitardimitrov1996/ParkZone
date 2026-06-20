package bg.softuni.parkzone.web.parkinglot;

import bg.softuni.parkzone.service.parkinglot.ParkingLotService;
import org.springframework.stereotype.Controller;

@Controller
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

}
