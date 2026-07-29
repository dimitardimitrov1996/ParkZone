package bg.softuni.parkzone.model.dto.reservation;

import bg.softuni.parkzone.model.entities.reservation.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationViewDTO {

    private Reservation reservation;

    private String invoiceStatus;
}
