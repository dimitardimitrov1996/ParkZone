package bg.softuni.parkzone.web.payment;

import bg.softuni.parkzone.exception.ApplicationException;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import bg.softuni.parkzone.model.dto.payment.PaymentRequestDTO;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.security.AuthenticationUserDetails;
import bg.softuni.parkzone.service.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/reservation/{id}")
    public ModelAndView getPaymentPage(@PathVariable UUID id,
                                       @AuthenticationPrincipal AuthenticationUserDetails principal,
                                       RedirectAttributes redirectAttributes) {

        UUID userId = principal.getId();

        try {
            InvoiceResponse invoice = paymentService.getInvoiceForReservationPayment(id, userId);
            Reservation reservation = paymentService.getReservationForPayment(id, userId);

            ModelAndView modelAndView = new ModelAndView("payments/pay");
            modelAndView.addObject("reservationId", id);
            modelAndView.addObject("invoice", invoice);
            modelAndView.addObject("reservation", reservation);
            modelAndView.addObject("paymentRequestDTO", PaymentRequestDTO.builder().build());

            return modelAndView;

        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return new ModelAndView("redirect:/reservations");
        }
    }

    @PostMapping("/reservation/{id}")
    public ModelAndView payReservationInvoice(
            @PathVariable UUID id,
            @Valid @ModelAttribute("paymentRequestDTO") PaymentRequestDTO paymentRequestDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticationUserDetails principal,
            RedirectAttributes redirectAttributes) {

        UUID userId = principal.getId();

        InvoiceResponse invoice;

        try {
            invoice = paymentService.getInvoiceForReservationPayment(id, userId);
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return new ModelAndView("redirect:/reservations");
        }

        if (bindingResult.hasErrors()) {
            return getPaymentView(id, userId, invoice, bindingResult);
        }

        try {
            paymentService.payReservationInvoice(id, userId, paymentRequestDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice paid successfully");

            return new ModelAndView("redirect:/reservations");

        } catch (ApplicationException e) {

            String message = e.getMessage().toLowerCase();

            if (message.contains("expiration")) {
                bindingResult.rejectValue("expirationDate", "expirationDate.error", e.getMessage());
            } else {
                bindingResult.reject("paymentError", e.getMessage());
            }

            return getPaymentView(id, userId, invoice, bindingResult);
        }
    }

    private ModelAndView getPaymentView(UUID reservationId,
                                        UUID userId,
                                        InvoiceResponse invoice,
                                        BindingResult bindingResult) {

        Reservation reservation = paymentService.getReservationForPayment(reservationId, userId);

        ModelAndView modelAndView = new ModelAndView("payments/pay", bindingResult.getModel());
        modelAndView.addObject("reservationId", reservationId);
        modelAndView.addObject("invoice", invoice);
        modelAndView.addObject("reservation", reservation);

        return modelAndView;
    }
}
