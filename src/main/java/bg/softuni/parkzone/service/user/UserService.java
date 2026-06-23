package bg.softuni.parkzone.service.user;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.dto.user.UserLoginRequestDTO;
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
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VehicleRepository vehicleRepository;
    private final ReservationRepository reservationRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, VehicleRepository vehicleRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.vehicleRepository = vehicleRepository;
        this.reservationRepository = reservationRepository;
    }


    public UserDTO register(UserRegisterRequestDTO userRegisterRequestDTO) {


        userRepository.findByUsername(userRegisterRequestDTO.getUsername()).ifPresent(user -> {
                throw new BusinessRuleException("Account with this username already exists");
        });

        userRepository.findByEmail(userRegisterRequestDTO.getEmail()).ifPresent(user -> {
            throw new BusinessRuleException("Account with this email already exists");
        });


        User user = User.builder()
                .username(userRegisterRequestDTO.getUsername())
                .password(passwordEncoder.encode(userRegisterRequestDTO.getPassword()))
                .email(userRegisterRequestDTO.getEmail())
                .isActive(true)
                .role(UserRole.USER)
                .build();

        userRepository.save(user);

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(UserRole.USER)
                .phoneNumber(user.getPhoneNumber())
                .isActive(true)
                .build();
    }

    public UserDTO login(@Valid UserLoginRequestDTO userLoginRequestDTO) {

        Optional<User> existingUser = userRepository.findByEmail(userLoginRequestDTO.getEmail());

        if(existingUser.isEmpty() ||
                !passwordEncoder.matches(userLoginRequestDTO.getPassword(), existingUser.get().getPassword())) {
            throw new BusinessRuleException("Invalid credentials, please try again");
        }

        User user = existingUser.get();

        if (!user.isActive()) {
            throw new BusinessRuleException("Your account is inactive. Please contact an administrator.");
        }

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .build();
    }

    public UserDTO findById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new BusinessRuleException("User not found"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.isActive())
                .build();
    }

    public boolean isAdmin(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        return user.getRole() == UserRole.ADMIN && user.isActive();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void toggleUserStatus(UUID userId, UUID currentAdminId) {

        if (userId.equals(currentAdminId)) {
            throw new BusinessRuleException("You cannot deactivate your own admin account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        if (user.isActive()) {
            user.setActive(false);

            List<Vehicle> userVehicles = vehicleRepository.findAllByOwnerId(userId);

            for (Vehicle vehicle : userVehicles) {
                vehicle.setActive(false);
            }

            vehicleRepository.saveAll(userVehicles);

            List<Reservation> activeReservations = reservationRepository
                    .findAllByUserIdAndStatus(userId, ReservationStatus.ACTIVE);

            for (Reservation reservation : activeReservations) {
                reservation.setStatus(ReservationStatus.CANCELLED);
            }

            reservationRepository.saveAll(activeReservations);

        } else {
            user.setActive(true);

            List<Vehicle> userVehicles = vehicleRepository.findAllByOwnerId(userId);

            for (Vehicle vehicle : userVehicles) {
                vehicle.setActive(true);
            }

            vehicleRepository.saveAll(userVehicles);
        }

        userRepository.save(user);
    }

    public UserProfileUpdateRequestDTO getUserProfileData(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        return UserProfileUpdateRequestDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public void updateUserProfile(UUID userId, UserProfileUpdateRequestDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        user.setFirstName(emptyToNull(dto.getFirstName()));
        user.setLastName(emptyToNull(dto.getLastName()));
        user.setPhoneNumber(emptyToNull(dto.getPhoneNumber()));

        userRepository.save(user);
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

}
