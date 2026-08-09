package bg.softuni.parkzone.web.admin;

import bg.softuni.parkzone.config.SecurityConfiguration;
import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.reservation.ReservationType;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.parkinglot.ParkingLotService;
import bg.softuni.parkzone.service.parkingspot.ParkingSpotService;
import bg.softuni.parkzone.service.reservation.ReservationService;
import bg.softuni.parkzone.service.user.AuthenticationUserDetailsService;
import bg.softuni.parkzone.service.user.UserService;
import bg.softuni.parkzone.service.vehicle.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(AdminController.class)
@Import(SecurityConfiguration.class)
class AdminControllerTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private ParkingLotService parkingLotService;

    @MockitoBean
    private ParkingSpotService parkingSpotService;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    private UUID adminId;
    private UUID userId;
    private UUID reservationId;
    private UUID parkingLotId;
    private UUID parkingSpotId;
    private UUID vehicleId;

    private AuthenticationUserDetails adminPrincipal;
    private AuthenticationUserDetails userPrincipal;

    private User admin;
    private User normalUser;
    private ParkingLot parkingLot;
    private ParkingSpot parkingSpot;
    private Vehicle vehicle;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        parkingLotId = UUID.randomUUID();
        parkingSpotId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();

        adminPrincipal = AuthenticationUserDetails.builder()
                .id(adminId)
                .username("admin@test.com")
                .password("encoded-password")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        userPrincipal = AuthenticationUserDetails.builder()
                .id(userId)
                .username("user@test.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        admin = User.builder()
                .id(adminId)
                .username("admin")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        normalUser = User.builder()
                .id(userId)
                .username("user")
                .email("user@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .phoneNumber("0888123456")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        parkingLot = ParkingLot.builder()
                .id(parkingLotId)
                .name("Indoor Parking")
                .parkingType(ParkingType.INDOOR)
                .capacity(30)
                .disabledParkingSpots(5)
                .electricChargingSpots(5)
                .dailyPrice(BigDecimal.valueOf(10))
                .monthlyPrice(BigDecimal.valueOf(240))
                .yearlyPrice(BigDecimal.valueOf(2400))
                .build();

        parkingSpot = ParkingSpot.builder()
                .id(parkingSpotId)
                .parkingLot(parkingLot)
                .spotNumber(8)
                .active(true)
                .disabledSpot(false)
                .electricChargingSpot(false)
                .build();

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .owner(normalUser)
                .registrationNumber("CA1234AA")
                .brand("Toyota")
                .model("Corolla")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(false)
                .active(true)
                .build();

        reservation = Reservation.builder()
                .id(reservationId)
                .user(normalUser)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .reservationType(ReservationType.DAILY)
                .status(ReservationStatus.ACTIVE)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .totalPrice(BigDecimal.valueOf(10))
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void getAdminDashboard_whenUserIsAdmin_shouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/admin").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    void getAdminDashboard_whenUserIsNotAdmin_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/admin").with(user(userPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersPage_shouldReturnUsersView() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(admin, normalUser));

        mockMvc.perform(get("/admin/users").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("users", List.of(admin, normalUser)))
                .andExpect(model().attribute("currentAdminId", adminId));
    }

    @Test
    void toggleUserStatus_whenSuccess_shouldRedirectWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/users/toggle-status/{id}", userId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/users"))
                .andExpect(flash().attribute("successMessage", "User status changed successfully"));

        verify(userService).toggleUserStatus(userId, adminId);
    }

    @Test
    void toggleUserStatus_whenServiceThrowsException_shouldRedirectWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("You cannot deactivate your own admin account"))
                .when(userService)
                .toggleUserStatus(userId, adminId);

        mockMvc.perform(post("/admin/users/toggle-status/{id}", userId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/users"))
                .andExpect(flash().attribute("errorMessage", "You cannot deactivate your own admin account"));
    }

    @Test
    void changeUserRole_whenSuccess_shouldRedirectWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/users/change-role/{id}", userId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/users"))
                .andExpect(flash().attribute("successMessage", "User role changed successfully"));

        verify(userService).changeUserRole(userId, adminId);
    }

    @Test
    void changeUserRole_whenServiceThrowsException_shouldRedirectWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("You cannot change your own role"))
                .when(userService)
                .changeUserRole(userId, adminId);

        mockMvc.perform(post("/admin/users/change-role/{id}", userId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/users"))
                .andExpect(flash().attribute("errorMessage", "You cannot change your own role"));
    }

    @Test
    void getAdminReservationsPage_shouldReturnReservationsView() throws Exception {
        when(reservationService.getAllReservations()).thenReturn(List.of(reservation));

        mockMvc.perform(get("/admin/reservations").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservations"))
                .andExpect(model().attribute("reservations", List.of(reservation)));
    }

    @Test
    void cancelReservation_whenSuccess_shouldRedirectToReservations() throws Exception {
        mockMvc.perform(post("/admin/reservations/cancel/{id}", reservationId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/reservations"));

        verify(reservationService).cancelReservationByAdmin(reservationId);
    }

    @Test
    void cancelReservation_whenServiceThrowsException_shouldRedirectWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("Only active reservations can be cancelled"))
                .when(reservationService)
                .cancelReservationByAdmin(reservationId);

        mockMvc.perform(post("/admin/reservations/cancel/{id}", reservationId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/reservations"))
                .andExpect(flash().attribute("errorMessage", "Only active reservations can be cancelled"));
    }

    @Test
    void getParkingLotsPage_shouldReturnParkingLotsView() throws Exception {
        when(parkingLotService.getAllParkingLots()).thenReturn(List.of(parkingLot));

        mockMvc.perform(get("/admin/parking-lots").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/parking-lots"))
                .andExpect(model().attribute("parkingLots", List.of(parkingLot)));
    }

    @Test
    void getParkingSpotsPage_shouldReturnParkingSpotsView() throws Exception {
        when(parkingLotService.getParkingLotById(parkingLotId)).thenReturn(parkingLot);
        when(parkingSpotService.getSpotsByParkingLot(parkingLotId)).thenReturn(List.of(parkingSpot));

        mockMvc.perform(get("/admin/parking-lots/{id}/spots", parkingLotId)
                        .with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/parking-spots"))
                .andExpect(model().attribute("parkingLot", parkingLot))
                .andExpect(model().attribute("parkingSpots", List.of(parkingSpot)));
    }

    @Test
    void makeDisabledSpot_whenSuccess_shouldRedirectToParkingLotSpots() throws Exception {
        when(parkingSpotService.makeDisabledSpot(parkingSpotId)).thenReturn(parkingLotId);

        mockMvc.perform(post("/admin/parking-spots/{id}/make-disabled", parkingSpotId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/parking-lots/" + parkingLotId + "/spots"));
    }

    @Test
    void makeElectricSpot_whenSuccess_shouldRedirectToParkingLotSpots() throws Exception {
        when(parkingSpotService.makeElectricChargingSpot(parkingSpotId)).thenReturn(parkingLotId);

        mockMvc.perform(post("/admin/parking-spots/{id}/make-electric", parkingSpotId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/parking-lots/" + parkingLotId + "/spots"));
    }

    @Test
    void makeNormalSpot_whenSuccess_shouldRedirectToParkingLotSpots() throws Exception {
        when(parkingSpotService.makeNormalSpot(parkingSpotId)).thenReturn(parkingLotId);

        mockMvc.perform(post("/admin/parking-spots/{id}/make-normal", parkingSpotId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/parking-lots/" + parkingLotId + "/spots"));
    }

    @Test
    void toggleActiveSpot_whenSuccess_shouldRedirectToParkingLotSpots() throws Exception {
        when(parkingSpotService.toggleActive(parkingSpotId)).thenReturn(parkingLotId);

        mockMvc.perform(post("/admin/parking-spots/{id}/toggle-active", parkingSpotId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/parking-lots/" + parkingLotId + "/spots"));
    }

    @Test
    void updateParkingSpot_whenServiceThrowsException_shouldRedirectToParkingLotsWithErrorMessage() throws Exception {
        when(parkingSpotService.makeDisabledSpot(parkingSpotId))
                .thenThrow(new BusinessRuleException("Parking spot cannot be changed"));

        mockMvc.perform(post("/admin/parking-spots/{id}/make-disabled", parkingSpotId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/parking-lots"))
                .andExpect(flash().attribute("errorMessage", "Parking spot cannot be changed"));
    }

    @Test
    void getVehiclesPage_shouldReturnVehiclesView() throws Exception {
        when(vehicleService.getAllVehicles()).thenReturn(List.of(vehicle));

        mockMvc.perform(get("/admin/vehicles").with(user(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vehicles"))
                .andExpect(model().attribute("vehicles", List.of(vehicle)));
    }

    @Test
    void deleteVehicle_whenSuccess_shouldRedirectWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/vehicles/delete/{id}", vehicleId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/vehicles"))
                .andExpect(flash().attribute("successMessage", "Vehicle deleted successfully"));

        verify(vehicleService).deleteVehicleByAdmin(vehicleId);
    }

    @Test
    void deleteVehicle_whenServiceThrowsException_shouldRedirectWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("Vehicle is already deleted"))
                .when(vehicleService)
                .deleteVehicleByAdmin(vehicleId);

        mockMvc.perform(post("/admin/vehicles/delete/{id}", vehicleId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/vehicles"))
                .andExpect(flash().attribute("errorMessage", "Vehicle is already deleted"));
    }

    @Test
    void activateVehicle_whenSuccess_shouldRedirectWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/vehicles/activate/{id}", vehicleId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/vehicles"))
                .andExpect(flash().attribute("successMessage", "Vehicle activated successfully"));

        verify(vehicleService).activateVehicleByAdmin(vehicleId);
    }

    @Test
    void activateVehicle_whenServiceThrowsException_shouldRedirectWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("Vehicle is already active"))
                .when(vehicleService)
                .activateVehicleByAdmin(vehicleId);

        mockMvc.perform(post("/admin/vehicles/activate/{id}", vehicleId)
                        .with(user(adminPrincipal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin/vehicles"))
                .andExpect(flash().attribute("errorMessage", "Vehicle is already active"));
    }
}
