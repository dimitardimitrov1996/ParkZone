package bg.softuni.parkzone.web;

import bg.softuni.parkzone.model.dto.user.UserDto;
import bg.softuni.parkzone.model.dto.user.UserLoginRequestDTO;
import bg.softuni.parkzone.model.dto.user.UserRegisterRequestDTO;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
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
    public ModelAndView  login(@ModelAttribute("userLoginRequest")
                                   @Valid UserLoginRequestDTO userLoginRequestDTO,
                               BindingResult bindingResult, HttpSession httpSession,
                               HttpServletResponse response) {


        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("login");
            return modelAndView;
        }

        UserDto user = userService.login(userLoginRequestDTO);
        httpSession.setAttribute("user_id", user.getId());

        if (userService.isAdmin(user.getId())) {
            return new ModelAndView("redirect:/admin");
        }

        return new ModelAndView("redirect:/home");
    }


    @GetMapping("/register")
    public ModelAndView getRegisterPage(Model model) {

        model.addAttribute("userRegisterRequestDTO", UserRegisterRequestDTO.builder().build());

        return new ModelAndView("register");
    }

    @PostMapping("/register")
    public ModelAndView register(@ModelAttribute("userRegisterRequest")
                                     @Valid UserRegisterRequestDTO userRegisterRequestDTO,
                                 BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return  new ModelAndView("register");
        }

        userService.register(userRegisterRequestDTO);

        return new ModelAndView("redirect:/login");
    }


    @GetMapping("/logout")
    public ModelAndView getLogoutPage(HttpSession httpSession) {
        httpSession.invalidate();
        return new ModelAndView("redirect:/");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(HttpSession httpSession) {

        UserDto user = userService.findById((UUID) httpSession.getAttribute("user_id"));

        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);

        return modelAndView;
    }



}
