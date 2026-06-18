package bg.softuni.parkzone.web;

import bg.softuni.parkzone.model.dto.user.UserDto;
import bg.softuni.parkzone.model.dto.user.UserLoginRequest;
import bg.softuni.parkzone.model.dto.user.UserRegisterRequest;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
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
    public ModelAndView getLoginPage() {
        UserLoginRequest userLoginRequest = UserLoginRequest.builder().build();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("userLoginRequest", userLoginRequest);

        return modelAndView;
    }

    @PostMapping("/login")
    public ModelAndView  login(@ModelAttribute("userLoginRequest")
                                   @Valid UserLoginRequest userLoginRequest,
                               BindingResult bindingResult, HttpSession httpSession, HttpServletResponse httpServletResponse) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("login");
            return modelAndView;
        }

        UserDto user = userService.login(userLoginRequest);
        httpSession.setAttribute("user_id", user.getId());

        return new ModelAndView("redirect:/home");
    }


    @GetMapping("/register")
    public ModelAndView getRegisterPage() {

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder().build();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("userRegisterRequest", userRegisterRequest);

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView register(@ModelAttribute("userRegisterRequest") @Valid UserRegisterRequest userRegisterRequest,
                                 BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("register");
            return  modelAndView;
        }

        userService.register(userRegisterRequest);

        return new  ModelAndView("redirect:/login");
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
