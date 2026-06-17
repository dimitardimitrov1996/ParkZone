package bg.softuni.parkzone.model.dto.parkinglot;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ParkingLotDto {

    private UUID id;
    private String name;
    private ParkingType type;
    private Integer capacity;
    private BigDecimal dailyPrice;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;


}
