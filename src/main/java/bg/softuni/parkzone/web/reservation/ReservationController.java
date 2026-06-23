package bg.softuni.parkzone.web.reservation;

import bg.softuni.parkzone.model.dto.reservation.ReservationCreateRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationEditRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
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

        reservationService.completeExpiredReservations();

        UUID userId = (UUID) session.getAttribute("user_id");

        UserDTO user = userService.findById(userId);

        List<Reservation> reservations = reservationService.getReservationsByUserId(userId);

        ModelAndView modelAndView = new ModelAndView("reservations/list");

        modelAndView.addObject("user", user);
        modelAndView.addObject("reservations", reservations);

        return modelAndView;
    }

    @GetMapping("/create")
    public ModelAndView getCreateReservationPage(HttpSession session) {

        UserDTO user = userService.findById((UUID) session.getAttribute("user_id"));

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
        UserDTO user = userService.findById(userId);

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
            UserDTO user,
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

    @PostMapping("/cancel/{id}")
    public ModelAndView cancelReservation(@PathVariable UUID id,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {

        UUID userId = (UUID) session.getAttribute("user_id");

        try {
            reservationService.cancelReservationByUser(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/reservations");
    }

    @GetMapping("/edit/{id}")
    public ModelAndView getEditReservationPage(@PathVariable UUID id,
                                               HttpSession session,
                                               RedirectAttributes redirectAttributes) {

        UUID userId = (UUID) session.getAttribute("user_id");
        UserDTO user = userService.findById(userId);

        try {
            ReservationEditRequestDTO reservationEditRequestDTO =
                    reservationService.getReservationForEdit(id, userId);

            List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(userId);
            List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();
            List<ParkingSpot> parkingSpots = parkingSpotService.getAllActiveParkingSpots();

            boolean reservationStarted = reservationService.isReservationStarted(id, userId);

            ModelAndView modelAndView = new ModelAndView("reservations/edit");
            modelAndView.addObject("reservationId", id);
            modelAndView.addObject("reservationEditRequestDTO", reservationEditRequestDTO);
            modelAndView.addObject("vehicles", vehicles);
            modelAndView.addObject("parkingLots", parkingLots);
            modelAndView.addObject("parkingSpots", parkingSpots);
            modelAndView.addObject("reservationStarted", reservationStarted);
            modelAndView.addObject("user", user);

            return modelAndView;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return new ModelAndView("redirect:/reservations");
        }
    }

    @PutMapping("/edit/{id}")
    public ModelAndView editReservation(
            @PathVariable UUID id,
            @Valid @ModelAttribute("reservationEditRequestDTO") ReservationEditRequestDTO reservationEditRequestDTO,
            BindingResult bindingResult,
            HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");
        UserDTO user = userService.findById(userId);

        boolean reservationStarted = !reservationEditRequestDTO.getStartDate().isAfter(LocalDateTime.now());

        if (bindingResult.hasErrors()) {

            List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(userId);
            List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();
            List<ParkingSpot> parkingSpots = parkingSpotService.getAllActiveParkingSpots();

            ModelAndView modelAndView = new ModelAndView("reservations/edit", bindingResult.getModel());
            modelAndView.addObject("reservationId", id);
            modelAndView.addObject("vehicles", vehicles);
            modelAndView.addObject("parkingLots", parkingLots);
            modelAndView.addObject("parkingSpots", parkingSpots);
            modelAndView.addObject("reservationStarted", reservationStarted);
            modelAndView.addObject("user", user);

            return modelAndView;
        }

        try {
            reservationService.editReservation(reservationEditRequestDTO, id, userId);

        } catch (IllegalArgumentException e) {

            String message = e.getMessage().toLowerCase();

            if (message.contains("start date")) {
                bindingResult.rejectValue("startDate", "startDate.error", e.getMessage());
            } else if (message.contains("end date")) {
                bindingResult.rejectValue("endDate", "endDate.error", e.getMessage());
            } else if (message.contains("reservation type")) {
                bindingResult.rejectValue("reservationType", "reservationType.error", e.getMessage());
            } else if (message.contains("vehicle already")
                    || message.contains("vehicle is not active")
                    || message.contains("cannot use this vehicle")) {
                bindingResult.rejectValue("vehicleId", "vehicleId.error", e.getMessage());
            } else if (message.contains("indoor") || message.contains("vans")) {
                bindingResult.rejectValue("parkingLotId", "parkingLotId.error", e.getMessage());
            } else if (message.contains("parking spot")
                    || message.contains("spot")
                    || message.contains("reserved")
                    || message.contains("disabled")
                    || message.contains("electric")) {
                bindingResult.rejectValue("parkingSpotId", "parkingSpotId.error", e.getMessage());
            } else {
                bindingResult.reject("reservationEditError", e.getMessage());
            }

            List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(userId);
            List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();
            List<ParkingSpot> parkingSpots = parkingSpotService.getAllActiveParkingSpots();

            ModelAndView modelAndView = new ModelAndView("reservations/edit", bindingResult.getModel());
            modelAndView.addObject("reservationId", id);
            modelAndView.addObject("vehicles", vehicles);
            modelAndView.addObject("parkingLots", parkingLots);
            modelAndView.addObject("parkingSpots", parkingSpots);
            modelAndView.addObject("reservationStarted", reservationStarted);
            modelAndView.addObject("user", user);

            return modelAndView;
        }

        return new ModelAndView("redirect:/reservations");
    }


}
