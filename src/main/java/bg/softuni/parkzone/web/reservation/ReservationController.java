package bg.softuni.parkzone.web.reservation;

import bg.softuni.parkzone.exception.ApplicationException;
import bg.softuni.parkzone.model.dto.reservation.ReservationCreateRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationEditRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationViewDTO;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.parkinglot.ParkingLotService;
import bg.softuni.parkzone.service.parkingspot.ParkingSpotService;
import bg.softuni.parkzone.service.reservation.ReservationService;
import bg.softuni.parkzone.service.user.UserService;
import bg.softuni.parkzone.service.vehicle.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    public ReservationController(ReservationService reservationService,
                                 UserService userService,
                                 VehicleService vehicleService,
                                 ParkingLotService parkingLotService,
                                 ParkingSpotService parkingSpotService) {
        this.reservationService = reservationService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.parkingLotService = parkingLotService;
        this.parkingSpotService = parkingSpotService;
    }

    @GetMapping
    public ModelAndView getReservationsByOwner(@AuthenticationPrincipal AuthenticationUserDetails principal) {

        UUID userId = principal.getId();

        UserDTO user = userService.findById(userId);

        List<ReservationViewDTO> reservationViews =
                reservationService.getReservationViewsByUserId(userId);

        ModelAndView modelAndView = new ModelAndView("reservations/list");
        modelAndView.addObject("user", user);
        modelAndView.addObject("reservationViews", reservationViews);

        return modelAndView;
    }

    @GetMapping("/create")
    public ModelAndView getCreateReservationPage(@AuthenticationPrincipal AuthenticationUserDetails principal) {

        UUID userId = principal.getId();
        UserDTO user = userService.findById(userId);

        ModelAndView modelAndView = new ModelAndView("reservations/create");
        modelAndView.addObject("reservationCreateRequestDTO", ReservationCreateRequestDTO.builder().build());

        addReservationFormData(modelAndView, userId, user);

        return modelAndView;
    }

    @PostMapping("/create")
    public ModelAndView createReservation(
            @Valid @ModelAttribute("reservationCreateRequestDTO") ReservationCreateRequestDTO reservationCreateRequestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticationUserDetails principal) {

        UUID userId = principal.getId();
        UserDTO user = userService.findById(userId);

        if (bindingResult.hasErrors()) {
            return getCreateReservationView(userId, user, bindingResult);
        }

        try {
            reservationService.createReservation(reservationCreateRequestDTO, userId);
        } catch (ApplicationException e) {
            rejectCreateReservationError(bindingResult, e);
            return getCreateReservationView(userId, user, bindingResult);
        }

        return new ModelAndView("redirect:/reservations");
    }

    @PostMapping("/cancel/{id}")
    public ModelAndView cancelReservation(@PathVariable UUID id,
                                          @AuthenticationPrincipal AuthenticationUserDetails principal,
                                          RedirectAttributes redirectAttributes) {

        UUID userId = principal.getId();

        try {
            reservationService.cancelReservationByUser(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled successfully");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/reservations");
    }

    @GetMapping("/edit/{id}")
    public ModelAndView getEditReservationPage(@PathVariable UUID id,
                                               @AuthenticationPrincipal AuthenticationUserDetails principal,
                                               RedirectAttributes redirectAttributes) {

        UUID userId = principal.getId();
        UserDTO user = userService.findById(userId);

        try {
            ReservationEditRequestDTO reservationEditRequestDTO =
                    reservationService.getReservationForEdit(id, userId);

            ModelAndView modelAndView = new ModelAndView("reservations/edit");
            modelAndView.addObject("reservationId", id);
            modelAndView.addObject("reservationEditRequestDTO", reservationEditRequestDTO);

            addReservationFormData(modelAndView, userId, user);
            addEditReservationState(modelAndView, id, userId);

            return modelAndView;

        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return new ModelAndView("redirect:/reservations");
        }
    }

    @PutMapping("/edit/{id}")
    public ModelAndView editReservation(
            @PathVariable UUID id,
            @Valid @ModelAttribute("reservationEditRequestDTO") ReservationEditRequestDTO reservationEditRequestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticationUserDetails principal) {

        UUID userId = principal.getId();
        UserDTO user = userService.findById(userId);

        if (bindingResult.hasErrors()) {
            return getEditReservationView(id, userId, user, bindingResult);
        }

        try {
            reservationService.editReservation(reservationEditRequestDTO, id, userId);
        } catch (ApplicationException e) {
            rejectEditReservationError(bindingResult, e);
            return getEditReservationView(id, userId, user, bindingResult);
        }

        return new ModelAndView("redirect:/reservations");
    }

    private ModelAndView getCreateReservationView(UUID userId,
                                                  UserDTO user,
                                                  BindingResult bindingResult) {

        ModelAndView modelAndView = new ModelAndView(
                "reservations/create",
                bindingResult.getModel()
        );

        addReservationFormData(modelAndView, userId, user);

        return modelAndView;
    }

    private ModelAndView getEditReservationView(UUID reservationId,
                                                UUID userId,
                                                UserDTO user,
                                                BindingResult bindingResult) {

        ModelAndView modelAndView = new ModelAndView(
                "reservations/edit",
                bindingResult.getModel()
        );

        modelAndView.addObject("reservationId", reservationId);

        addReservationFormData(modelAndView, userId, user);
        addEditReservationState(modelAndView, reservationId, userId);

        return modelAndView;
    }

    private void addReservationFormData(ModelAndView modelAndView,
                                        UUID userId,
                                        UserDTO user) {

        modelAndView.addObject("user", user);
        modelAndView.addObject("vehicles", vehicleService.getVehiclesByOwner(userId));
        modelAndView.addObject("parkingLots", parkingLotService.getAllParkingLots());
        modelAndView.addObject("parkingSpots", parkingSpotService.getAllActiveParkingSpots());
    }

    private void addEditReservationState(ModelAndView modelAndView,
                                         UUID reservationId,
                                         UUID userId) {

        ReservationStatus reservationStatus =
                reservationService.getReservationStatus(reservationId, userId);

        boolean pendingPayment = reservationStatus == ReservationStatus.PENDING_PAYMENT;
        boolean reservationStarted = reservationService.isReservationStarted(reservationId, userId);

        modelAndView.addObject("pendingPayment", pendingPayment);
        modelAndView.addObject("reservationStarted", reservationStarted);
    }

    private void rejectCreateReservationError(BindingResult bindingResult,
                                              ApplicationException e) {

        String message = e.getMessage();
        String lowerMessage = message.toLowerCase();

        bindingResult.reject("reservationError", message);

        if (lowerMessage.contains("end date")
                || lowerMessage.contains("daily")
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

        } else if (lowerMessage.contains("vehicle already")) {

            bindingResult.rejectValue("vehicleId", "vehicleId.error", message);
        }
    }

    private void rejectEditReservationError(BindingResult bindingResult,
                                            ApplicationException e) {

        String message = e.getMessage();
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("start date")) {

            bindingResult.rejectValue("startDate", "startDate.error", message);

        } else if (lowerMessage.contains("end date")) {

            bindingResult.rejectValue("endDate", "endDate.error", message);

        } else if (lowerMessage.contains("reservation type")) {

            bindingResult.rejectValue("reservationType", "reservationType.error", message);

        } else if (lowerMessage.contains("vehicle already")
                || lowerMessage.contains("vehicle is not active")
                || lowerMessage.contains("cannot use this vehicle")) {

            bindingResult.rejectValue("vehicleId", "vehicleId.error", message);

        } else if (lowerMessage.contains("indoor")
                || lowerMessage.contains("vans")) {

            bindingResult.rejectValue("parkingLotId", "parkingLotId.error", message);

        } else if (lowerMessage.contains("parking spot")
                || lowerMessage.contains("spot")
                || lowerMessage.contains("reserved")
                || lowerMessage.contains("disabled")
                || lowerMessage.contains("electric")) {

            bindingResult.rejectValue("parkingSpotId", "parkingSpotId.error", message);

        } else {

            bindingResult.reject("reservationEditError", message);
        }
    }
}