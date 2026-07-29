package bg.softuni.parkzone.service.user;

import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class AuthenticationUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AuthenticationUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with this email was not found"));

        return AuthenticationUserDetails.builder()
                .id(user.getId())
                .username(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .isActive(user.isActive())
                .build();
    }

}
