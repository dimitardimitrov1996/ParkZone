package bg.softuni.parkzone.web.vehicle;

import bg.softuni.parkzone.config.SecurityConfiguration;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.dto.vehicle.VehicleEditDTO;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
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
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(VehicleController.class)
@Import(SecurityConfiguration.class)
class VehicleControllerTest {

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    private UUID userId;
    private UUID vehicleId;

    private AuthenticationUserDetails principal;
    private UserDTO userDTO;
    private User user;
    private Vehicle vehicle;
    private VehicleEditDTO vehicleEditDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();

        principal = AuthenticationUserDetails.builder()
                .id(userId)
                .username("user@test.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        userDTO = UserDTO.builder()
                .id(userId)
                .username("testUser")
                .email("user@test.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        user = User.builder()
                .id(userId)
                .username("testUser")
                .email("user@test.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .registrationNumber("CA1234AA")
                .brand("Toyota")
                .model("Corolla")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(false)
                .owner(user)
                .active(true)
                .build();

        vehicleEditDTO = VehicleEditDTO.builder()
                .registrationNumber("CA1234AA")
                .brand("Toyota")
                .model("Corolla")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(false)
                .build();
    }

    @Test
    void getAllVehicles_shouldReturnVehicleListView() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        when(vehicleService.getVehiclesByOwner(userId)).thenReturn(List.of(vehicle));

        mockMvc.perform(get("/vehicles").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/list"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().attribute("vehicles", List.of(vehicle)));
    }

    @Test
    void getCreateVehiclePage_shouldReturnCreateView() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);

        mockMvc.perform(get("/vehicles/create").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/create"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().attributeExists("vehicleCreateRequestDTO"));
    }

    @Test
    void createNewVehicle_whenDataIsValid_shouldCreateVehicleAndRedirect() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);

        mockMvc.perform(validCreateRequest())
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/vehicles"));

        verify(vehicleService).createVehicle(any(), eq(userId));
    }

    @Test
    void createNewVehicle_whenValidationFails_shouldReturnCreateView() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);

        MockHttpServletRequestBuilder request = post("/vehicles/create")
                .param("registrationNumber", "invalid")
                .param("brand", "Toyota123")
                .param("model", "")
                .param("vehicleType", "")
                .param("engineType", "")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/create"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors(
                        "vehicleCreateRequestDTO",
                        "registrationNumber",
                        "brand",
                        "model",
                        "vehicleType",
                        "engineType"
                ));

        verify(vehicleService, never()).createVehicle(any(), any());
    }

    @Test
    void createNewVehicle_whenRegistrationAlreadyExists_shouldReturnCreateViewWithRegistrationError() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);

        doThrow(new IllegalArgumentException("Vehicle with this registration number already exists"))
                .when(vehicleService)
                .createVehicle(any(), eq(userId));

        mockMvc.perform(validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/create"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().attributeHasFieldErrors("vehicleCreateRequestDTO", "registrationNumber"));
    }

    @Test
    void createNewVehicle_whenOtherBusinessError_shouldReturnCreateViewWithGlobalError() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);

        doThrow(new IllegalArgumentException("Something went wrong"))
                .when(vehicleService)
                .createVehicle(any(), eq(userId));

        mockMvc.perform(validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/create"))
                .andExpect(model().attribute("user", userDTO))
                .andExpect(model().hasErrors());
    }

    @Test
    void getEditVehiclePage_shouldReturnEditView() throws Exception {
        when(vehicleService.getVehicleForEdit(vehicleId, userId)).thenReturn(vehicleEditDTO);

        mockMvc.perform(get("/vehicles/edit/{id}", vehicleId).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().attribute("vehicleId", vehicleId))
                .andExpect(model().attribute("vehicleEditDTO", vehicleEditDTO));
    }

    @Test
    void editVehicle_whenDataIsValid_shouldEditVehicleAndRedirect() throws Exception {
        mockMvc.perform(validEditRequest())
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/vehicles"));

        verify(vehicleService).editVehicle(any(), eq(vehicleId), eq(userId));
    }

    @Test
    void editVehicle_whenValidationFails_shouldReturnEditView() throws Exception {
        MockHttpServletRequestBuilder request = put("/vehicles/edit/{id}", vehicleId)
                .param("registrationNumber", "invalid")
                .param("brand", "BMW123")
                .param("model", "")
                .param("vehicleType", "")
                .param("engineType", "")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().attribute("vehicleId", vehicleId))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors(
                        "vehicleEditDTO",
                        "registrationNumber",
                        "brand",
                        "model",
                        "vehicleType",
                        "engineType"
                ));

        verify(vehicleService, never()).editVehicle(any(), any(), any());
    }

    @Test
    void editVehicle_whenRegistrationError_shouldReturnEditViewWithRegistrationError() throws Exception {
        doThrow(new IllegalArgumentException("Vehicle with this registration number already exists"))
                .when(vehicleService)
                .editVehicle(any(), eq(vehicleId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().attribute("vehicleId", vehicleId))
                .andExpect(model().attributeHasFieldErrors("vehicleEditDTO", "registrationNumber"));
    }

    @Test
    void editVehicle_whenElectricError_shouldReturnEditViewWithEngineTypeError() throws Exception {
        doThrow(new IllegalArgumentException("This vehicle has an active reservation for an electric charging spot"))
                .when(vehicleService)
                .editVehicle(any(), eq(vehicleId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().attributeHasFieldErrors("vehicleEditDTO", "engineType"));
    }

    @Test
    void editVehicle_whenDisabledError_shouldReturnEditViewWithDisabledError() throws Exception {
        doThrow(new IllegalArgumentException("This vehicle has an active reservation for a disabled parking spot"))
                .when(vehicleService)
                .editVehicle(any(), eq(vehicleId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().attributeHasFieldErrors("vehicleEditDTO", "disabledParkingRequired"));
    }

    @Test
    void editVehicle_whenIndoorOrVanError_shouldReturnEditViewWithVehicleTypeError() throws Exception {
        doThrow(new IllegalArgumentException("This vehicle has an active indoor reservation and cannot be changed to VAN"))
                .when(vehicleService)
                .editVehicle(any(), eq(vehicleId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().attributeHasFieldErrors("vehicleEditDTO", "vehicleType"));
    }

    @Test
    void editVehicle_whenOtherBusinessError_shouldReturnEditViewWithGlobalError() throws Exception {
        doThrow(new IllegalArgumentException("Something went wrong"))
                .when(vehicleService)
                .editVehicle(any(), eq(vehicleId), eq(userId));

        mockMvc.perform(validEditRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("vehicles/edit"))
                .andExpect(model().hasErrors());
    }

    @Test
    void deleteVehicle_shouldDeleteVehicleAndRedirect() throws Exception {
        mockMvc.perform(post("/vehicles/delete/{id}", vehicleId)
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/vehicles"));

        verify(vehicleService).deleteVehicle(vehicleId, userId);
    }

    private MockHttpServletRequestBuilder validCreateRequest() {
        return post("/vehicles/create")
                .param("registrationNumber", "CA1234AA")
                .param("brand", "Toyota")
                .param("model", "Corolla")
                .param("vehicleType", "CAR")
                .param("engineType", "ELECTRIC")
                .param("disabledParkingRequired", "false")
                .with(user(principal))
                .with(csrf());
    }

    private MockHttpServletRequestBuilder validEditRequest() {
        return put("/vehicles/edit/{id}", vehicleId)
                .param("registrationNumber", "CB5678BB")
                .param("brand", "Honda")
                .param("model", "Civic")
                .param("vehicleType", "CAR")
                .param("engineType", "ELECTRIC")
                .param("disabledParkingRequired", "false")
                .with(user(principal))
                .with(csrf());
    }
}