package bg.softuni.parkzone.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ModelAndView handleApplicationException(ApplicationException exception) {

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorCode", exception.getErrorCode());
        modelAndView.addObject("title", exception.getErrorTitle());
        modelAndView.addObject("message", exception.getMessage());

        return modelAndView;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDeniedException() {

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorCode", "403");
        modelAndView.addObject("title", "Access denied");
        modelAndView.addObject("message", "You do not have permission to access this page.");

        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException() {

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorCode", "500");
        modelAndView.addObject("title", "Something went wrong");
        modelAndView.addObject("message", "Please try again later.");

        return modelAndView;
    }
}
