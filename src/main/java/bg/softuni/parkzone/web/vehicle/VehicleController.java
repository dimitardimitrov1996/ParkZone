package bg.softuni.parkzone.web.vehicle;

import bg.softuni.parkzone.model.dto.user.UserDTO;
import bg.softuni.parkzone.model.dto.vehicle.VehicleCreateRequestDTO;
import bg.softuni.parkzone.model.dto.vehicle.VehicleEditDTO;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.service.user.UserService;
import bg.softuni.parkzone.service.vehicle.VehicleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;


@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserService userService;

    public VehicleController(VehicleService vehicleService, UserService userService) {
        this.vehicleService = vehicleService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getAllVehicles(HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        UserDTO user = userService.findById(userId);

        List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(userId);

        ModelAndView modelAndView = new ModelAndView("vehicles/list");

        modelAndView.addObject("user", user);
        modelAndView.addObject("vehicles", vehicles);

        return modelAndView;
    }

    @GetMapping("/create")
    public ModelAndView getCreateVehiclePage(HttpSession session) {

        UserDTO user = userService.findById((UUID) session.getAttribute("user_id"));

        ModelAndView modelAndView = new ModelAndView("vehicles/create");

        modelAndView.addObject("user", user);
        modelAndView.addObject("vehicleCreateRequestDTO", VehicleCreateRequestDTO.builder().build());

        return modelAndView;
    }

    @PostMapping("/create")
    public ModelAndView createNewVehicle(@Valid VehicleCreateRequestDTO vehicleCreateRequestDTO,
                                      BindingResult bindingResult,
                                      HttpSession session) {

        UserDTO user = userService.findById((UUID) session.getAttribute("user_id"));

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView(
                    "vehicles/create",
                    bindingResult.getModel()
            );

            modelAndView.addObject("user", user);

            return modelAndView;
        }

        try {
            vehicleService.createVehicle(vehicleCreateRequestDTO, user.getId());
        } catch (IllegalArgumentException e) {

            String message = e.getMessage().toLowerCase();

            if (message.contains("registration")) {
                bindingResult.rejectValue(
                        "registrationNumber",
                        "registrationNumber.error",
                        e.getMessage()
                );
            } else {
                bindingResult.reject("vehicleCreateError", e.getMessage());
            }

            ModelAndView modelAndView = new ModelAndView(
                    "vehicles/create",
                    bindingResult.getModel()
            );

            modelAndView.addObject("user", user);

            return modelAndView;
        }

        return new ModelAndView("redirect:/vehicles");

    }

    @GetMapping("/edit/{id}")
    public ModelAndView getEditVehiclePage(@PathVariable UUID id, HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        VehicleEditDTO vehicleEditDTO = vehicleService.getVehicleForEdit(id, userId);

        ModelAndView modelAndView = new ModelAndView("vehicles/edit");
        modelAndView.addObject("vehicleId", id);
        modelAndView.addObject("vehicleEditDTO", vehicleEditDTO);

        return modelAndView;
    }

    @PutMapping("/edit/{id}")
    public ModelAndView editVehicle(
            @PathVariable UUID id,
            @Valid @ModelAttribute("vehicleEditDTO") VehicleEditDTO vehicleEditDTO,
            BindingResult bindingResult,
            HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("vehicles/edit", bindingResult.getModel());
            modelAndView.addObject("vehicleId", id);
            return modelAndView;
        }

        try {
            vehicleService.editVehicle(vehicleEditDTO, id, userId);
        } catch (IllegalArgumentException e) {

            String message = e.getMessage().toLowerCase();

            if (message.contains("registration")) {
                bindingResult.rejectValue("registrationNumber", "registrationNumber.error", e.getMessage());
            } else if (message.contains("electric")) {
                bindingResult.rejectValue("engineType", "engineType.error", e.getMessage());
            } else if (message.contains("disabled")) {
                bindingResult.rejectValue("disabledParkingRequired", "disabledParkingRequired.error", e.getMessage());
            } else if (message.contains("indoor") || message.contains("van")) {
                bindingResult.rejectValue("vehicleType", "vehicleType.error", e.getMessage());
            } else {
                bindingResult.reject("vehicleEditError", e.getMessage());
            }

            ModelAndView modelAndView = new ModelAndView("vehicles/edit", bindingResult.getModel());
            modelAndView.addObject("vehicleId", id);
            return modelAndView;
        }

        return new ModelAndView("redirect:/vehicles");
    }

    @PostMapping("/delete/{id}")
    public ModelAndView deleteVehicle(@PathVariable UUID id,
                                      HttpSession session) {

        UUID userId = (UUID) session.getAttribute("user_id");

        vehicleService.deleteVehicle(id, userId);

        return new ModelAndView("redirect:/vehicles");
    }


}
