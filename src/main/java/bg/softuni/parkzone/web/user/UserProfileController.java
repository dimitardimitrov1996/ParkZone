package bg.softuni.parkzone.web.user;

import bg.softuni.parkzone.model.dto.user.UserProfileUpdateRequestDTO;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class UserProfileController {
    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public ModelAndView getProfilePage(HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        UserProfileUpdateRequestDTO profileDTO = userService.getUserProfileData(userId);

        ModelAndView modelAndView = new ModelAndView("user/profile");
        modelAndView.addObject("userProfileUpdateRequestDTO", profileDTO);

        return modelAndView;
    }

    @PostMapping()
    public ModelAndView updateProfile(
            @Valid @ModelAttribute("userProfileUpdateRequestDTO") UserProfileUpdateRequestDTO userProfileUpdateRequestDTO,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("user/profile", bindingResult.getModel());
        }

        UUID userId = (UUID) session.getAttribute("user_id");

        userService.updateUserProfile(userId, userProfileUpdateRequestDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");

        return new ModelAndView("redirect:/profile");
    }


}
