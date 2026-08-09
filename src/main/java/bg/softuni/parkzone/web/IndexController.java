package bg.softuni.parkzone.web;

import bg.softuni.parkzone.exception.ApplicationException;
import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.dto.user.UserLoginRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserRegisterRequestDTO;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

    private final UserService userService;

    public IndexController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(required = false) String error,
                                     @RequestParam(required = false) String disabled,
                                     Model model) {

        model.addAttribute("userLoginRequestDTO", UserLoginRequestDTO.builder().build());

        ModelAndView modelAndView = new ModelAndView("login");

        if (disabled != null) {
            modelAndView.addObject("loginError", "Your account is inactive. Please contact an administrator.");
        } else if (error != null) {
            modelAndView.addObject("loginError", "Invalid email or password");
        }

        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage(Model model) {

        model.addAttribute("userRegisterRequestDTO", UserRegisterRequestDTO.builder().build());

        return new ModelAndView("register");
    }

    @PostMapping("/register")
    public ModelAndView register(@ModelAttribute("userRegisterRequestDTO")
                                 @Valid UserRegisterRequestDTO userRegisterRequestDTO,
                                 BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("register", bindingResult.getModel());
        }

        try {
            userService.register(userRegisterRequestDTO);
        } catch (ApplicationException e) {

            String message = e.getMessage().toLowerCase();

            if (message.contains("username")) {
                bindingResult.rejectValue("username", "username.error", e.getMessage());
            } else if (message.contains("email")) {
                bindingResult.rejectValue("email", "email.error", e.getMessage());
            } else {
                bindingResult.reject("registerError", e.getMessage());
            }

            return new ModelAndView("register", bindingResult.getModel());
        }

        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(@AuthenticationPrincipal AuthenticationUserDetails principal) {

        UserDTO user = userService.findById(principal.getId());

        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }

}
