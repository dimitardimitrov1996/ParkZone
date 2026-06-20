package bg.softuni.parkzone.model.entities.parkinglot;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "parking_lots")
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParkingType parkingType;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int disabledParkingSpots;

    @Column(nullable = false)
    private int electricChargingSpots;

    @Column(nullable = false)
    private BigDecimal dailyPrice;

    @Column(nullable = false)
    private BigDecimal monthlyPrice;

    @Column(nullable = false)
    private BigDecimal yearlyPrice;


}
