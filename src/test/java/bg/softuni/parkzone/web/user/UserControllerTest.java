package bg.softuni.parkzone.web.user;

import bg.softuni.parkzone.config.SecurityConfiguration;
import bg.softuni.parkzone.model.dto.user.UserProfileUpdateRequestDTO;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.user.AuthenticationUserDetailsService;
import bg.softuni.parkzone.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(UserProfileController.class)
@Import(SecurityConfiguration.class)
class UserProfileControllerTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    private UUID userId;
    private AuthenticationUserDetails principal;
    private UserProfileUpdateRequestDTO profileDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        principal = AuthenticationUserDetails.builder()
                .id(userId)
                .username("user@test.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        profileDTO = UserProfileUpdateRequestDTO.builder()
                .username("testuser")
                .email("user@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .phoneNumber("0888123456")
                .build();
    }

    @Test
    void getProfilePage_shouldReturnProfileView() throws Exception {
        when(userService.getUserProfileData(userId))
                .thenReturn(profileDTO);

        mockMvc.perform(get("/profile").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attribute("userProfileUpdateRequestDTO", profileDTO));
    }

    @Test
    void updateProfile_whenDataIsValid_shouldUpdateProfileAndRedirect() throws Exception {
        MockHttpServletRequestBuilder request = post("/profile")
                .param("username", "testuser")
                .param("email", "user@test.com")
                .param("firstName", "Petar")
                .param("lastName", "Petrov")
                .param("phoneNumber", "0888999888")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/profile"))
                .andExpect(flash().attribute("successMessage", "Profile updated successfully"));

        verify(userService).updateUserProfile(eq(userId), any());
    }

    @Test
    void updateProfile_whenValidationFails_shouldReturnProfileView() throws Exception {
        MockHttpServletRequestBuilder request = post("/profile")
                .param("username", "testuser")
                .param("email", "user@test.com")
                .param("firstName", "Ivan123")
                .param("lastName", "Petrov123")
                .param("phoneNumber", "123")
                .with(user(principal))
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors(
                        "userProfileUpdateRequestDTO",
                        "firstName",
                        "lastName",
                        "phoneNumber"
                ));

        verify(userService, never()).updateUserProfile(any(), any());
    }
}