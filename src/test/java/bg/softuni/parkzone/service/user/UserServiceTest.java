package bg.softuni.parkzone.service.user;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.dto.user.UserProfileUpdateRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserRegisterRequestDTO;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.repository.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UUID adminId;
    private User user;
    private User admin;
    private UserRegisterRequestDTO registerDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("user@test.com")
                .password("encoded-password")
                .firstName("Ivan")
                .lastName("Ivanov")
                .phoneNumber("0888123456")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        admin = User.builder()
                .id(adminId)
                .username("adminuser")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        registerDTO = UserRegisterRequestDTO.builder()
                .username("newuser")
                .email("new@test.com")
                .password("123456")
                .build();
    }

    @Test
    void register_whenDataIsValid_shouldCreateUser() {
        when(userRepository.findByUsername(registerDTO.getUsername()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerDTO.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerDTO.getPassword()))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(userId);
                    return savedUser;
                });

        UserDTO result = userService.register(registerDTO);

        assertEquals(userId, result.getId());
        assertEquals(registerDTO.getUsername(), result.getUsername());
        assertEquals(registerDTO.getEmail(), result.getEmail());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.isActive());

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getUsername().equals(registerDTO.getUsername())
                        && savedUser.getEmail().equals(registerDTO.getEmail())
                        && savedUser.getPassword().equals("encoded-password")
                        && savedUser.getRole() == UserRole.USER
                        && savedUser.isActive()
        ));
    }

    @Test
    void register_whenUsernameExists_shouldThrowException() {
        when(userRepository.findByUsername(registerDTO.getUsername()))
                .thenReturn(Optional.of(user));

        assertThrows(BusinessRuleException.class,
                () -> userService.register(registerDTO));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenEmailExists_shouldThrowException() {
        when(userRepository.findByUsername(registerDTO.getUsername()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(registerDTO.getEmail()))
                .thenReturn(Optional.of(user));

        assertThrows(BusinessRuleException.class,
                () -> userService.register(registerDTO));

        verify(userRepository, never()).save(any());
    }

    @Test
    void findById_whenUserExists_shouldReturnUserDTO() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserDTO result = userService.findById(userId);

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(user.getRole(), result.getRole());
        assertEquals(user.isActive(), result.isActive());
    }

    @Test
    void findById_whenUserDoesNotExist_shouldThrowException() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> userService.findById(userId));
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        when(userRepository.findAll())
                .thenReturn(List.of(user, admin));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals(user, result.get(0));
        assertEquals(admin, result.get(1));
    }

    @Test
    void toggleUserStatus_whenUserIsActive_shouldDeactivateUserVehiclesAndReservations() {
        Vehicle vehicle = Vehicle.builder()
                .active(true)
                .owner(user)
                .build();

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.ACTIVE)
                .user(user)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(vehicleRepository.findAllByOwnerId(userId))
                .thenReturn(List.of(vehicle));
        when(reservationRepository.findAllByUserIdAndStatus(userId, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        userService.toggleUserStatus(userId, adminId);

        assertFalse(user.isActive());
        assertFalse(vehicle.isActive());
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

        verify(vehicleRepository).saveAll(List.of(vehicle));
        verify(reservationRepository).saveAll(List.of(reservation));
        verify(userRepository).save(user);
    }

    @Test
    void toggleUserStatus_whenUserIsInactive_shouldActivateUserAndVehicles() {
        user.setActive(false);

        Vehicle vehicle = Vehicle.builder()
                .active(false)
                .owner(user)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(vehicleRepository.findAllByOwnerId(userId))
                .thenReturn(List.of(vehicle));

        userService.toggleUserStatus(userId, adminId);

        assertTrue(user.isActive());
        assertTrue(vehicle.isActive());

        verify(vehicleRepository).saveAll(List.of(vehicle));
        verify(reservationRepository, never()).saveAll(anyList());
        verify(userRepository).save(user);
    }

    @Test
    void toggleUserStatus_whenAdminTriesToDeactivateOwnAccount_shouldThrowException() {
        assertThrows(BusinessRuleException.class,
                () -> userService.toggleUserStatus(adminId, adminId));

        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleUserStatus_whenUserDoesNotExist_shouldThrowException() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> userService.toggleUserStatus(userId, adminId));
    }

    @Test
    void getUserProfileData_whenUserExists_shouldReturnProfileDTO() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserProfileUpdateRequestDTO result = userService.getUserProfileData(userId);

        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getFirstName(), result.getFirstName());
        assertEquals(user.getLastName(), result.getLastName());
        assertEquals(user.getPhoneNumber(), result.getPhoneNumber());
    }

    @Test
    void getUserProfileData_whenUserDoesNotExist_shouldThrowException() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> userService.getUserProfileData(userId));
    }

    @Test
    void updateUserProfile_whenDataIsValid_shouldUpdateProfile() {
        UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
                .firstName("  Petar  ")
                .lastName("  Petrov  ")
                .phoneNumber("  0888999888  ")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        userService.updateUserProfile(userId, dto);

        assertEquals("Petar", user.getFirstName());
        assertEquals("Petrov", user.getLastName());
        assertEquals("0888999888", user.getPhoneNumber());

        verify(userRepository).save(user);
    }

    @Test
    void updateUserProfile_whenFieldsAreBlank_shouldSaveNullValues() {
        UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
                .firstName(" ")
                .lastName("")
                .phoneNumber(null)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        userService.updateUserProfile(userId, dto);

        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getPhoneNumber());

        verify(userRepository).save(user);
    }

    @Test
    void updateUserProfile_whenUserDoesNotExist_shouldThrowException() {
        UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
                .firstName("Petar")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> userService.updateUserProfile(userId, dto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserRole_whenUserIsUser_shouldChangeRoleToAdmin() {
        user.setRole(UserRole.USER);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        userService.changeUserRole(userId, adminId);

        assertEquals(UserRole.ADMIN, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void changeUserRole_whenUserIsAdmin_shouldChangeRoleToUser() {
        user.setRole(UserRole.ADMIN);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        userService.changeUserRole(userId, adminId);

        assertEquals(UserRole.USER, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void changeUserRole_whenAdminTriesToChangeOwnRole_shouldThrowException() {
        assertThrows(BusinessRuleException.class,
                () -> userService.changeUserRole(adminId, adminId));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserRole_whenUserDoesNotExist_shouldThrowException() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> userService.changeUserRole(userId, adminId));

        verify(userRepository, never()).save(any());
    }
}
