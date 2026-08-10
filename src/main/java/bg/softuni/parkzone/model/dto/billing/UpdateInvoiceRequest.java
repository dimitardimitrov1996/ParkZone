package bg.softuni.parkzone.model.dto.billing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpdateInvoiceRequest {

    private BigDecimal amount;

    private String currency;
}