package bg.softuni.parkzone.model.entities.parkingsport;

import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "parking_spots",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"parking_lot_id", "spot_number"}
        )
)
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "spot_number", nullable = false)
    private int spotNumber;

    @Column(nullable = false)
    private boolean disabledSpot;

    @Column(nullable = false)
    private boolean electricChargingSpot;

    @Column(nullable = false)
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;


}
