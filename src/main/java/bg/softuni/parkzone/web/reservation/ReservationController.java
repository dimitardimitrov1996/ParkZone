package bg.softuni.parkzone.web.reservation;

import bg.softuni.parkzone.model.dto.reservation.ReservationCreateRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserDto;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkingsport.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.service.parkinglot.ParkingLotService;
import bg.softuni.parkzone.service.parkingspot.ParkingSpotService;
import bg.softuni.parkzone.service.reservation.ReservationService;
import bg.softuni.parkzone.service.user.UserService;
import bg.softuni.parkzone.service.vehicle.VehicleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final ParkingLotService parkingLotService;
    private final ParkingSpotService parkingSpotService;

    public ReservationController(ReservationService reservationService, UserService userService, VehicleService vehicleService, ParkingLotService parkingLotService, ParkingSpotService parkingSpotService) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.parkingLotService = parkingLotService;
        this.parkingSpotService = parkingSpotService;
    }

    @GetMapping
    public ModelAndView getReservationsByOwner(HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        UserDto user = userService.findById(userId);

        List<Reservation> reservations = reservationService.getReservationsByUserId(userId);

        ModelAndView modelAndView = new ModelAndView("reservations/list");

        modelAndView.addObject("user", user);
        modelAndView.addObject("reservations", reservations);

        return modelAndView;
    }

    @GetMapping("/create")
    public ModelAndView getCreateReservationPage(HttpSession session) {

        UserDto user = userService.findById((UUID) session.getAttribute("user_id"));

        List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(user.getId());
        List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();
        List<ParkingSpot> parkingSpots = parkingSpotService.getAllActiveParkingSpots();

        ModelAndView modelAndView = new ModelAndView("reservations/create");

        modelAndView.addObject("user", user);
        modelAndView.addObject("reservationCreateRequestDTO", ReservationCreateRequestDTO.builder().build());
        modelAndView.addObject("vehicles", vehicles);
        modelAndView.addObject("parkingLots", parkingLots);
        modelAndView.addObject("parkingSpots", parkingSpots);

        return modelAndView;

    }

    @PostMapping("/create")
    public ModelAndView createReservation(
            @Valid @ModelAttribute("reservationCreateRequestDTO") ReservationCreateRequestDTO reservationCreateRequestDTO,
            BindingResult bindingResult,
            HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");
        UserDto user = userService.findById(userId);

        if (bindingResult.hasErrors()) {
            return getCreateReservationView(userId, user, bindingResult);
        }

        try {
            reservationService.createReservation(reservationCreateRequestDTO, userId);
        } catch (IllegalArgumentException e) {

            String message = e.getMessage();
            String lowerMessage = message.toLowerCase();
            System.out.println("RESERVATION ERROR: " + message);

            bindingResult.reject("reservationError", message);

            if (lowerMessage.contains("end date")
                    || lowerMessage.contains("monthly")
                    || lowerMessage.contains("yearly")) {

                bindingResult.rejectValue("endDate", "endDate.error", message);

            } else if (lowerMessage.contains("indoor parking")) {

                bindingResult.rejectValue("parkingLotId", "parkingLotId.error", message);

            } else if (lowerMessage.contains("parking spot")
                    || lowerMessage.contains("selected parking spot")
                    || lowerMessage.contains("reserved")
                    || lowerMessage.contains("disabled")
                    || lowerMessage.contains("electric")) {

                bindingResult.rejectValue("parkingSpotId", "parkingSpotId.error", message);
            }  else if (lowerMessage.contains("vehicle already")) {

                bindingResult.rejectValue("vehicleId", "vehicleId.error", message);
            }

            return getCreateReservationView(userId, user, bindingResult);
        }

        return new ModelAndView("redirect:/reservations");
    }

    private ModelAndView getCreateReservationView(
            UUID userId,
            UserDto user,
            BindingResult bindingResult) {

        List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(userId);
        List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();
        List<ParkingSpot> parkingSpots = parkingSpotService.getAllActiveParkingSpots();

        ModelAndView modelAndView = new ModelAndView(
                "reservations/create",
                bindingResult.getModel()
        );

        modelAndView.addObject("user", user);
        modelAndView.addObject("vehicles", vehicles);
        modelAndView.addObject("parkingLots", parkingLots);
        modelAndView.addObject("parkingSpots", parkingSpots);

        return modelAndView;
    }


}
