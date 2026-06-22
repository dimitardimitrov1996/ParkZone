package bg.softuni.parkzone.web;

import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.dto.user.UserLoginRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserRegisterRequestDTO;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

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
    public ModelAndView getLoginPage(Model model) {

        model.addAttribute("userLoginRequestDTO", UserLoginRequestDTO.builder().build());

        return new ModelAndView("login");
    }

    @PostMapping("/login")
    public ModelAndView  login(@ModelAttribute("userLoginRequestDTO")
                                   @Valid UserLoginRequestDTO userLoginRequestDTO,
                               BindingResult bindingResult, HttpSession httpSession) {


        if (bindingResult.hasErrors()) {
            return new ModelAndView("login", bindingResult.getModel());
        }

        try {
            UserDTO user = userService.login(userLoginRequestDTO);
            httpSession.setAttribute("user_id", user.getId());

            if (userService.isAdmin(user.getId())) {
                return new ModelAndView("redirect:/admin");
            }

            return new ModelAndView("redirect:/home");

        } catch (IllegalArgumentException e) {
            bindingResult.reject("loginError", e.getMessage());
            return new ModelAndView("login", bindingResult.getModel());
        }
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
            return  new ModelAndView("register");
        }

        try {
            userService.register(userRegisterRequestDTO);
        } catch (IllegalArgumentException e) {

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


    @GetMapping("/logout")
    public ModelAndView getLogoutPage(HttpSession httpSession) {
        httpSession.invalidate();
        return new ModelAndView("redirect:/");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(HttpSession httpSession) {

        UserDTO user = userService.findById((UUID) httpSession.getAttribute("user_id"));

        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }



}
