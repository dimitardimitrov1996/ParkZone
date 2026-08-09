package bg.softuni.parkzone.web;

import bg.softuni.parkzone.config.SecurityConfiguration;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.entities.user.UserRole;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.user.AuthenticationUserDetailsService;
import bg.softuni.parkzone.service.user.UserService;
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
@WebMvcTest(IndexController.class)
@Import(SecurityConfiguration.class)
class IndexControllerTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationUserDetailsService authenticationUserDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIndexPage_shouldReturnIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void getLoginPage_shouldReturnLoginViewWithLoginDto() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("userLoginRequestDTO"))
                .andExpect(model().attributeDoesNotExist("loginError"));
    }

    @Test
    void getLoginPage_whenErrorParamExists_shouldReturnLoginError() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("userLoginRequestDTO"))
                .andExpect(model().attribute("loginError", "Invalid email or password"));
    }

    @Test
    void getLoginPage_whenDisabledParamExists_shouldReturnDisabledMessage() throws Exception {
        mockMvc.perform(get("/login").param("disabled", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("userLoginRequestDTO"))
                .andExpect(model().attribute(
                        "loginError",
                        "Your account is inactive. Please contact an administrator."
                ));
    }

    @Test
    void getRegisterPage_shouldReturnRegisterViewWithRegisterDto() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("userRegisterRequestDTO"));
    }

    @Test
    void register_whenDataIsValid_shouldRegisterUserAndRedirectToLogin() throws Exception {
        MockHttpServletRequestBuilder request = post("/register")
                .param("username", "testuser")
                .param("email", "test@test.com")
                .param("password", "123456")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login"));

        verify(userService).register(any());
    }

    @Test
    void register_whenValidationFails_shouldReturnRegisterView() throws Exception {
        MockHttpServletRequestBuilder request = post("/register")
                .param("username", "abc")
                .param("email", "invalid-email")
                .param("password", "123")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors(
                        "userRegisterRequestDTO",
                        "username",
                        "email",
                        "password"
                ));

        verify(userService, never()).register(any());
    }

    @Test
    void register_whenUsernameAlreadyExists_shouldReturnRegisterViewWithUsernameError() throws Exception {
        doThrow(new IllegalArgumentException("Account with this username already exists"))
                .when(userService)
                .register(any());

        MockHttpServletRequestBuilder request = post("/register")
                .param("username", "testuser")
                .param("email", "test@test.com")
                .param("password", "123456")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userRegisterRequestDTO", "username"));
    }

    @Test
    void register_whenEmailAlreadyExists_shouldReturnRegisterViewWithEmailError() throws Exception {
        doThrow(new IllegalArgumentException("Account with this email already exists"))
                .when(userService)
                .register(any());

        MockHttpServletRequestBuilder request = post("/register")
                .param("username", "testuser")
                .param("email", "test@test.com")
                .param("password", "123456")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("userRegisterRequestDTO", "email"));
    }

    @Test
    void register_whenServiceThrowsOtherException_shouldReturnRegisterViewWithGlobalError() throws Exception {
        doThrow(new IllegalArgumentException("Something went wrong"))
                .when(userService)
                .register(any());

        MockHttpServletRequestBuilder request = post("/register")
                .param("username", "testuser")
                .param("email", "test@test.com")
                .param("password", "123456")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void getHomePage_whenUserIsAuthenticated_shouldReturnHomeViewWithUser() throws Exception {
        UUID userId = UUID.randomUUID();

        AuthenticationUserDetails principal = AuthenticationUserDetails.builder()
                .id(userId)
                .username("user@test.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        UserDTO userDTO = UserDTO.builder()
                .id(userId)
                .username("testuser")
                .email("user@test.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userService.findById(userId))
                .thenReturn(userDTO);

        mockMvc.perform(get("/home").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("user", userDTO));
    }
}
