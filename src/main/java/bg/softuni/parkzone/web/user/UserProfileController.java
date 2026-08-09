package bg.softuni.parkzone.web.user;

import bg.softuni.parkzone.model.dto.user.UserProfileUpdateRequestDTO;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping
    public ModelAndView getProfilePage(@AuthenticationPrincipal AuthenticationUserDetails principal) {

        UUID userId = principal.getId();

        UserProfileUpdateRequestDTO profileDTO = userService.getUserProfileData(userId);

        ModelAndView modelAndView = new ModelAndView("user/profile");
        modelAndView.addObject("userProfileUpdateRequestDTO", profileDTO);

        return modelAndView;
    }

    @PostMapping
    public ModelAndView updateProfile(
            @Valid @ModelAttribute("userProfileUpdateRequestDTO") UserProfileUpdateRequestDTO userProfileUpdateRequestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticationUserDetails principal,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("user/profile", bindingResult.getModel());
        }

        UUID userId = principal.getId();

        userService.updateUserProfile(userId, userProfileUpdateRequestDTO);

        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");

        return new ModelAndView("redirect:/profile");
    }
}
