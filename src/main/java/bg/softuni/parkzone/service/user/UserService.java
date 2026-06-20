package bg.softuni.parkzone.service.user;

import bg.softuni.parkzone.model.dto.user.UserDto;
import bg.softuni.parkzone.model.dto.user.UserLoginRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserRegisterRequestDTO;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.repository.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public UserDto register(UserRegisterRequestDTO userRegisterRequestDTO) {


        userRepository.findByUsername(userRegisterRequestDTO.getUsername()).ifPresent(user -> {
                throw new RuntimeException("Account with this username already exists");
        });

        userRepository.findByEmail(userRegisterRequestDTO.getEmail()).ifPresent(user -> {
            throw new RuntimeException("Account with this email already exists");
        });


        User user = User.builder()
                .username(userRegisterRequestDTO.getUsername())
                .password(passwordEncoder.encode(userRegisterRequestDTO.getPassword()))
                .email(userRegisterRequestDTO.getEmail())
                .isActive(true)
                .role(UserRole.USER)
                .build();

        userRepository.save(user);

        return UserDto.builder()
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

    public UserDto login(@Valid UserLoginRequestDTO userLoginRequestDTO) {

        Optional<User> existingUser = userRepository.findByEmail(userLoginRequestDTO.getEmail());

        if(existingUser.isEmpty() ||
                !passwordEncoder.matches(userLoginRequestDTO.getPassword(), existingUser.get().getPassword())) {
            throw new RuntimeException("Invalid credentials, please try again");
        }

        User user = existingUser.get();

        return UserDto.builder()
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

    public UserDto findById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()
                -> new RuntimeException("User not found"));
        return UserDto.builder()
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
}
