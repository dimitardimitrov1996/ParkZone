package bg.softuni.parkzone.service.billing.client;

import bg.softuni.parkzone.model.dto.billing.CreateInvoiceRequest;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "billing-service",
        url = "http://localhost:8081/api/v1/invoices"
)
public interface BillingClient {

    @PostMapping
    InvoiceResponse createInvoice(@RequestBody CreateInvoiceRequest request);

    @GetMapping("/reservation/{reservationId}")
    InvoiceResponse getInvoiceByReservationId(@PathVariable UUID reservationId);

    @PutMapping("/{invoiceId}/pay")
    InvoiceResponse payInvoice(@PathVariable UUID invoiceId);

    @PutMapping("/reservation/{reservationId}/cancel")
    InvoiceResponse cancelInvoiceByReservationId(@PathVariable UUID reservationId);
}
