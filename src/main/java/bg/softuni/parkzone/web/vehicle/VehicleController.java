package bg.softuni.parkzone.web.vehicle;

import bg.softuni.parkzone.model.dto.user.UserDto;
import bg.softuni.parkzone.model.dto.vehicle.VehicleCreateRequest;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.service.user.UserService;
import bg.softuni.parkzone.service.vehicle.VehicleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

        UserDto user = userService.findById(userId);

        List<Vehicle> vehicles = vehicleService.getVehiclesByOwner(userId);

        ModelAndView modelAndView = new ModelAndView("vehicles/list");

        modelAndView.addObject("user", user);
        modelAndView.addObject("vehicles", vehicles);

        return modelAndView;
    }

    @GetMapping("/create")
    public ModelAndView GetCreateVehiclePage(VehicleCreateRequest vehicleCreateRequest,
                                      BindingResult bindingResult,
                                      HttpSession session) {

        UserDto user = userService.findById((UUID) session.getAttribute("user_id"));

       if  (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("vehicles/create");
            modelAndView.addObject("vehicleCreateRequest", vehicleCreateRequest);
            modelAndView.addObject("user", user);
            return modelAndView;
        } else {
           return new ModelAndView("redirect:/vehicles");
       }
    }

    @PostMapping("/create")
    public ModelAndView createNewVehicle(@Valid VehicleCreateRequest vehicleCreateRequest,
                                      BindingResult bindingResult,
                                      HttpSession session) {

        UserDto user = userService.findById((UUID) session.getAttribute("user_id"));

        if  (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("vehicles/create");
            modelAndView.addObject("vehicleCreateRequest", vehicleCreateRequest);
            modelAndView.addObject("user", user);
            return modelAndView;
        }

        vehicleService.createVehicle(vehicleCreateRequest, user.getId());

        return new ModelAndView("redirect:/vehicles");

    }



}
