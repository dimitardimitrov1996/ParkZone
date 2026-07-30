package bg.softuni.parkzone.web.admin;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.parkinglot.ParkingLotService;
import bg.softuni.parkzone.service.parkingspot.ParkingSpotService;
import bg.softuni.parkzone.service.reservation.ReservationService;
import bg.softuni.parkzone.service.user.UserService;
import bg.softuni.parkzone.service.vehicle.VehicleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ReservationService reservationService;
    private final ParkingLotService parkingLotService;
    private final ParkingSpotService parkingSpotService;
    private final VehicleService vehicleService;

    public AdminController(UserService userService, ReservationService reservationService, ParkingLotService parkingLotService, ParkingSpotService parkingSpotService, VehicleService vehicleService) {
        this.userService = userService;
        this.reservationService = reservationService;
        this.parkingLotService = parkingLotService;
        this.parkingSpotService = parkingSpotService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ModelAndView getAdminDashboard() {
        return new ModelAndView("admin/dashboard");
    }

    @GetMapping("/users")
    public ModelAndView getUsersPage(@AuthenticationPrincipal AuthenticationUserDetails principal) {

        List<User> users = userService.getAllUsers();

        ModelAndView modelAndView = new ModelAndView("admin/users");
        modelAndView.addObject("users", users);
        modelAndView.addObject("currentAdminId", principal.getId());

        return modelAndView;
    }

    @PostMapping("/users/toggle-status/{id}")
    public ModelAndView toggleUserStatus(@PathVariable UUID id,
                                         @AuthenticationPrincipal AuthenticationUserDetails principal,
                                         RedirectAttributes redirectAttributes) {

        UUID currentAdminId = principal.getId();

        try {
            userService.toggleUserStatus(id, currentAdminId);
            redirectAttributes.addFlashAttribute("successMessage", "User status changed successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/admin/users");
    }

    @GetMapping("/reservations")
    public ModelAndView getAdminReservationsPage() {

        reservationService.completeExpiredReservations();

        List<Reservation> reservations = reservationService.getAllReservations();

        ModelAndView modelAndView = new ModelAndView("admin/reservations");
        modelAndView.addObject("reservations", reservations);

        return modelAndView;
    }

    @PostMapping("/reservations/cancel/{id}")
    public ModelAndView cancelReservation(@PathVariable UUID id,
                                          RedirectAttributes redirectAttributes) {

        try {
            reservationService.cancelReservationByAdmin(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/admin/reservations");
    }

    @GetMapping("/parking-lots")
    public ModelAndView getParkingLotsPage() {

        List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();

        ModelAndView modelAndView = new ModelAndView("admin/parking-lots");
        modelAndView.addObject("parkingLots", parkingLots);

        return modelAndView;
    }

    @GetMapping("/parking-lots/{id}/spots")
    public ModelAndView getParkingSpotsPage(@PathVariable UUID id) {

        ParkingLot parkingLot = parkingLotService.getParkingLotById(id);
        List<ParkingSpot> parkingSpots = parkingSpotService.getSpotsByParkingLot(id);

        ModelAndView modelAndView = new ModelAndView("admin/parking-spots");
        modelAndView.addObject("parkingLot", parkingLot);
        modelAndView.addObject("parkingSpots", parkingSpots);

        return modelAndView;
    }

    @PostMapping("/parking-spots/{id}/make-disabled")
    public ModelAndView makeDisabledSpot(@PathVariable UUID id,
                                         RedirectAttributes redirectAttributes) {
        return updateParkingSpot(id, redirectAttributes, "disabled");
    }

    @PostMapping("/parking-spots/{id}/make-electric")
    public ModelAndView makeElectricSpot(@PathVariable UUID id,
                                         RedirectAttributes redirectAttributes) {
        return updateParkingSpot(id, redirectAttributes, "electric");
    }

    @PostMapping("/parking-spots/{id}/make-normal")
    public ModelAndView makeNormalSpot(@PathVariable UUID id,
                                       RedirectAttributes redirectAttributes) {
        return updateParkingSpot(id, redirectAttributes, "normal");
    }

    @PostMapping("/parking-spots/{id}/toggle-active")
    public ModelAndView toggleActiveSpot(@PathVariable UUID id,
                                         RedirectAttributes redirectAttributes) {
        return updateParkingSpot(id, redirectAttributes, "active");
    }

    private ModelAndView updateParkingSpot(UUID id,
                                           RedirectAttributes redirectAttributes,
                                           String action) {

        UUID parkingLotId;

        try {
            parkingLotId = switch (action) {
                case "disabled" -> parkingSpotService.makeDisabledSpot(id);
                case "electric" -> parkingSpotService.makeElectricChargingSpot(id);
                case "normal" -> parkingSpotService.makeNormalSpot(id);
                case "active" -> parkingSpotService.toggleActive(id);
                default -> throw new IllegalArgumentException("Invalid parking spot action");
            };
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

            return new ModelAndView("redirect:/admin/parking-lots");
        }

        return new ModelAndView("redirect:/admin/parking-lots/" + parkingLotId + "/spots");
    }

    @GetMapping("/vehicles")
    public ModelAndView getVehiclesPage() {

        List<Vehicle> vehicles = vehicleService.getAllVehicles();

        ModelAndView modelAndView = new ModelAndView("admin/vehicles");
        modelAndView.addObject("vehicles", vehicles);

        return modelAndView;
    }

    @PostMapping("/vehicles/delete/{id}")
    public ModelAndView deleteVehicle(@PathVariable UUID id,
                                      RedirectAttributes redirectAttributes) {

        try {
            vehicleService.deleteVehicleByAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle deleted successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/admin/vehicles");
    }

    @PostMapping("/vehicles/activate/{id}")
    public ModelAndView activateVehicle(@PathVariable UUID id,
                                        RedirectAttributes redirectAttributes) {

        try {
            vehicleService.activateVehicleByAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle activated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/admin/vehicles");
    }

    @PostMapping("/users/change-role/{id}")
    public ModelAndView changeUserRole(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticationUserDetails principal,
                                       RedirectAttributes redirectAttributes) {

        try {
            userService.changeUserRole(id, principal.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User role changed successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return new ModelAndView("redirect:/admin/users");
    }



}
