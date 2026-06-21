package bg.softuni.parkzone.web.admin;

import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.service.reservation.ReservationService;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.servlet.http.HttpSession;
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

    public AdminController(UserService userService, ReservationService reservationService) {
        this.userService = userService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public ModelAndView getAdminDashboard(HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        if (!userService.isAdmin(userId)) {
            return new ModelAndView("redirect:/home");
        }

        return new ModelAndView("admin/dashboard");
    }

    @GetMapping("/users")
    public ModelAndView getUsersPage() {

        List<User> users = userService.getAllUsers();

        ModelAndView modelAndView = new ModelAndView("admin/users");
        modelAndView.addObject("users", users);

        return modelAndView;
    }

    @PostMapping("/users/toggle-status/{id}")
    public ModelAndView toggleUserStatus(@PathVariable UUID id,
                                         HttpSession session) {

        UUID currentAdminId = (UUID) session.getAttribute("user_id");

        userService.toggleUserStatus(id, currentAdminId);

        return new ModelAndView("redirect:/admin/users");
    }

    @GetMapping("/reservations")
    public ModelAndView getAdminReservationsPage() {

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

}
