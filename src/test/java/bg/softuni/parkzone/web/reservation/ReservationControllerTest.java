package bg.softuni.parkzone.web.reservation;

import bg.softuni.parkzone.config.SecurityConfiguration;
import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.dto.reservation.ReservationEditRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationViewDTO;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.reservation.ReservationType;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(ReservationController.class)
@Import(SecurityConfiguration.class)
class ReservationControllerTest {

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private ParkingLotService parkingLotService;

    @MockitoBean
    private ParkingSpotService parkingSpotService;

    @MockitoBean
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    private UUID userId;
    private UUID reservationId;
    private UUID vehicleId;
    private UUID parkingLotId;
    private UUID parkingSpotId;

    private AuthenticationUserDetails principal;
    private UserDTO userDTO;
    private Vehicle vehicle;
    private ParkingLot parkingLot;
    private ParkingSpot parkingSpot;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        parkingLotId = UUID.randomUUID();
        parkingSpotId = UUID.randomUUID();

        principal = AuthenticationUserDetails.builder()
                .id(userId)
                .username("user@test.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        userDTO = UserDTO.builder()
                .id(userId)
                .username("testuser")
                .email("user@test.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .registrationNumber("CA1234AA")
                .brand("Tesla")
                .model("Model 3")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .active(true)
                .build();

        parkingLot = ParkingLot.builder()
                .id(parkingLotId)
                .name("Indoor Parking")
                .parkingType(ParkingType.INDOOR)
                .dailyPrice(BigDecimal.valueOf(5))
                .monthlyPrice(BigDecimal.valueOf(240))
                .yearlyPrice(BigDecimal.valueOf(2400))
                .build();

        parkingSpot = ParkingSpot.builder()
                .id(parkingSpotId)
                .parkingLot(parkingLot)
                .spotNumber(8)
                .active(true)
                .build();

        reservation = Reservation.builder()
                .id(reservationId)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .reservationType(ReservationType.MONTHLY)
                .status(ReservationStatus.PENDING_PAYMENT)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusMonths(1))
                .totalPrice(BigDecimal.valueOf(240))
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void getReservationsByOwner_shouldReturnReservationsListView() throws Exception {
        ReservationViewDTO reservationView = ReservationViewDTO.builder()
                .reservation(reservation)
                .invoiceId(UUID.randomUUID())
                .invoiceStatus("PENDING")
                .build();

        when(userService.findById(userId)).thenReturn(userDTO);
        when(reservationService.getReservationViewsByUserId(userId))
                .thenReturn(List.of(reservationView));

        mockMvc.perform(get("/reservations").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/list"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().attribute("reservationViews", List.of(reservationView)));
    }

    @Test
    void getCreateReservationPage_shouldReturnCreateView() throws Exception {
        mockCreatePageData();

        mockMvc.perform(get("/reservations/create").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/create"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().attributeExists("reservationCreateRequestDTO"))
                .andExpect(model().attribute("vehicles", List.of(vehicle)))
                .andExpect(model().attribute("parkingLots", List.of(parkingLot)))
                .andExpect(model().attribute("parkingSpots", List.of(parkingSpot)));
    }

    @Test
    void createReservation_whenDataIsValid_shouldCreateReservationAndRedirect() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);

        MockHttpServletRequestBuilder request = post("/reservations/create")
                .param("vehicleId", vehicleId.toString())
                .param("parkingLotId", parkingLotId.toString())
                .param("parkingSpotId", parkingSpotId.toString())
                .param("reservationType", "DAILY")
                .param("startDate", LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString())
                .param("endDate", LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString())
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"));

        verify(reservationService).createReservation(any(), eq(userId));
    }

    @Test
    void createReservation_whenValidationFails_shouldReturnCreateView() throws Exception {
        mockCreatePageData();

        MockHttpServletRequestBuilder request = post("/reservations/create")
                .param("vehicleId", "")
                .param("parkingLotId", "")
                .param("parkingSpotId", "")
                .param("reservationType", "")
                .param("startDate", "")
                .param("endDate", "")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/create"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("vehicles", "parkingLots", "parkingSpots"));

        verify(reservationService, never()).createReservation(any(), any());
    }

    @Test
    void createReservation_whenServiceThrowsEndDateError_shouldReturnCreateViewWithEndDateError() throws Exception {
        mockCreatePageData();

        doThrow(new BusinessRuleException("End date must be after start date"))
                .when(reservationService)
                .createReservation(any(), eq(userId));

        mockMvc.perform(validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/create"))
                .andExpect(model().attributeHasFieldErrors("reservationCreateRequestDTO", "endDate"));
    }

    @Test
    void createReservation_whenServiceThrowsIndoorParkingError_shouldReturnCreateViewWithParkingLotError() throws Exception {
        mockCreatePageData();

        doThrow(new BusinessRuleException("Vans cannot use indoor parking"))
                .when(reservationService)
                .createReservation(any(), eq(userId));

        mockMvc.perform(validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/create"))
                .andExpect(model().attributeHasFieldErrors("reservationCreateRequestDTO", "parkingLotId"));
    }

    @Test
    void createReservation_whenServiceThrowsParkingSpotError_shouldReturnCreateViewWithParkingSpotError() throws Exception {
        mockCreatePageData();

        doThrow(new BusinessRuleException("Selected parking spot is reserved"))
                .when(reservationService)
                .createReservation(any(), eq(userId));

        mockMvc.perform(validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/create"))
                .andExpect(model().attributeHasFieldErrors("reservationCreateRequestDTO", "parkingSpotId"));
    }

    @Test
    void createReservation_whenServiceThrowsVehicleError_shouldReturnCreateViewWithVehicleError() throws Exception {
        mockCreatePageData();

        doThrow(new BusinessRuleException("Vehicle already has reservation"))
                .when(reservationService)
                .createReservation(any(), eq(userId));

        mockMvc.perform(validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/create"))
                .andExpect(model().attributeHasFieldErrors("reservationCreateRequestDTO", "vehicleId"));
    }

    @Test
    void cancelReservation_whenSuccess_shouldRedirectWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reservations/cancel/{id}", reservationId)
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"))
                .andExpect(flash().attribute("successMessage", "Reservation cancelled successfully"));

        verify(reservationService).cancelReservationByUser(reservationId, userId);
    }

    @Test
    void cancelReservation_whenServiceThrowsException_shouldRedirectWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("Only active reservations can be cancelled"))
                .when(reservationService)
                .cancelReservationByUser(reservationId, userId);

        mockMvc.perform(post("/reservations/cancel/{id}", reservationId)
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"))
                .andExpect(flash().attribute("errorMessage", "Only active reservations can be cancelled"));
    }

    @Test
    void getEditReservationPage_whenDataIsValid_shouldReturnEditView() throws Exception {
        ReservationEditRequestDTO editDTO = validEditDTO();

        when(userService.findById(userId)).thenReturn(userDTO);
        when(reservationService.getReservationForEdit(reservationId, userId))
                .thenReturn(editDTO);
        when(vehicleService.getVehiclesByOwner(userId)).thenReturn(List.of(vehicle));
        when(parkingLotService.getAllParkingLots()).thenReturn(List.of(parkingLot));
        when(parkingSpotService.getAllActiveParkingSpots()).thenReturn(List.of(parkingSpot));
        when(reservationService.isReservationStarted(reservationId, userId)).thenReturn(false);

        mockMvc.perform(get("/reservations/edit/{id}", reservationId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attribute("reservationId", reservationId))
                .andExpect(model().attribute("reservationEditRequestDTO", editDTO))
                .andExpect(model().attribute("reservationStarted", false))
                .andExpect(model().attribute("user", userDTO));
    }

    @Test
    void getEditReservationPage_whenServiceThrowsException_shouldRedirectToReservations() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        when(reservationService.getReservationForEdit(reservationId, userId))
                .thenThrow(new BusinessRuleException("Reservation not found"));

        mockMvc.perform(get("/reservations/edit/{id}", reservationId)
                        .with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"))
                .andExpect(flash().attribute("errorMessage", "Reservation not found"));
    }

    @Test
    void editReservation_whenDataIsValid_shouldEditAndRedirect() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        when(reservationService.isReservationStarted(reservationId, userId)).thenReturn(false);

        mockMvc.perform(validEditRequest())
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/reservations"));

        verify(reservationService).editReservation(any(), eq(reservationId), eq(userId));
    }

    @Test
    void editReservation_whenValidationFails_shouldReturnEditView() throws Exception {
        mockEditPageData();

        MockHttpServletRequestBuilder request = put("/reservations/edit/{id}", reservationId)
                .param("vehicleId", "")
                .param("parkingLotId", "")
                .param("parkingSpotId", "")
                .param("reservationType", "")
                .param("startDate", "")
                .param("endDate", "")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().hasErrors())
                .andExpect(model().attribute("reservationId", reservationId))
                .andExpect(model().attributeExists("vehicles", "parkingLots", "parkingSpots"));

        verify(reservationService, never()).editReservation(any(), any(), any());
    }

    @Test
    void editReservation_whenStartDateError_shouldReturnEditViewWithStartDateError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("Start date cannot be changed"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attributeHasFieldErrors("reservationEditRequestDTO", "startDate"));
    }

    @Test
    void editReservation_whenEndDateError_shouldReturnEditViewWithEndDateError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("End date is invalid"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attributeHasFieldErrors("reservationEditRequestDTO", "endDate"));
    }

    @Test
    void editReservation_whenReservationTypeError_shouldReturnEditViewWithReservationTypeError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("Reservation type cannot be changed"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attributeHasFieldErrors("reservationEditRequestDTO", "reservationType"));
    }

    @Test
    void editReservation_whenVehicleError_shouldReturnEditViewWithVehicleError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("Vehicle already has reservation"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attributeHasFieldErrors("reservationEditRequestDTO", "vehicleId"));
    }

    @Test
    void editReservation_whenIndoorError_shouldReturnEditViewWithParkingLotError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("Vans cannot use indoor parking"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attributeHasFieldErrors("reservationEditRequestDTO", "parkingLotId"));
    }

    @Test
    void editReservation_whenParkingSpotError_shouldReturnEditViewWithParkingSpotError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("Parking spot is reserved"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().attributeHasFieldErrors("reservationEditRequestDTO", "parkingSpotId"));
    }

    @Test
    void editReservation_whenOtherError_shouldReturnEditViewWithGlobalError() throws Exception {
        mockEditPageData();

        doThrow(new BusinessRuleException("Something went wrong"))
                .when(reservationService)
                .editReservation(any(), eq(reservationId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/edit"))
                .andExpect(model().hasErrors());
    }

    private void mockCreatePageData() {
        when(userService.findById(userId)).thenReturn(userDTO);
        when(vehicleService.getVehiclesByOwner(userId)).thenReturn(List.of(vehicle));
        when(parkingLotService.getAllParkingLots()).thenReturn(List.of(parkingLot));
        when(parkingSpotService.getAllActiveParkingSpots()).thenReturn(List.of(parkingSpot));
    }

    private void mockEditPageData() {
        when(userService.findById(userId)).thenReturn(userDTO);
        when(reservationService.isReservationStarted(reservationId, userId)).thenReturn(false);
        when(vehicleService.getVehiclesByOwner(userId)).thenReturn(List.of(vehicle));
        when(parkingLotService.getAllParkingLots()).thenReturn(List.of(parkingLot));
        when(parkingSpotService.getAllActiveParkingSpots()).thenReturn(List.of(parkingSpot));
    }

    private MockHttpServletRequestBuilder validCreateRequest() {
        return post("/reservations/create")
                .param("vehicleId", vehicleId.toString())
                .param("parkingLotId", parkingLotId.toString())
                .param("parkingSpotId", parkingSpotId.toString())
                .param("reservationType", "DAILY")
                .param("startDate", LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString())
                .param("endDate", LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString())
                .with(user(principal))
                .with(csrf());
    }

    private MockHttpServletRequestBuilder validEditRequest() {
        return put("/reservations/edit/{id}", reservationId)
                .param("vehicleId", vehicleId.toString())
                .param("parkingLotId", parkingLotId.toString())
                .param("parkingSpotId", parkingSpotId.toString())
                .param("reservationType", "DAILY")
                .param("startDate", LocalDateTime.now().plusDays(3).withSecond(0).withNano(0).toString())
                .param("endDate", LocalDateTime.now().plusDays(4).withSecond(0).withNano(0).toString())
                .with(user(principal))
                .with(csrf());
    }

    private ReservationEditRequestDTO validEditDTO() {
        return ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(ReservationType.DAILY)
                .startDate(LocalDateTime.now().plusDays(3))
                .endDate(LocalDateTime.now().plusDays(4))
                .build();
    }
}
