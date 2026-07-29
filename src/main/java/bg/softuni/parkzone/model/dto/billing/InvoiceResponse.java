package bg.softuni.parkzone.model.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;

    private UUID reservationId;

    private UUID userId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private LocalDateTime createdOn;

    private LocalDateTime paidOn;

    private LocalDateTime cancelledOn;
}
