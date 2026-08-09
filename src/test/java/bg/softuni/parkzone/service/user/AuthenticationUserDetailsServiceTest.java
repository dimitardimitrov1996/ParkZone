package bg.softuni.parkzone.service.user;

import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("user@test.com")
                .password("encoded-password")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
    }

    @Test
    void loadUserByUsername_whenEmailExists_shouldReturnAuthenticationUserDetails() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserDetails result = authenticationUserDetailsService.loadUserByUsername(user.getEmail());

        assertInstanceOf(AuthenticationUserDetails.class, result);

        AuthenticationUserDetails principal = (AuthenticationUserDetails) result;

        assertEquals(user.getId(), principal.getId());
        assertEquals(user.getEmail(), principal.getUsername());
        assertEquals(user.getPassword(), principal.getPassword());
        assertEquals(user.getRole(), principal.getRole());
        assertTrue(principal.isEnabled());
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_whenEmailDoesNotExist_shouldThrowException() {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> authenticationUserDetailsService.loadUserByUsername(user.getEmail()));
    }

    @Test
    void loadUserByUsername_whenUserIsInactive_shouldReturnDisabledPrincipal() {
        user.setActive(false);

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        UserDetails result = authenticationUserDetailsService.loadUserByUsername(user.getEmail());

        assertFalse(result.isEnabled());
    }
}
