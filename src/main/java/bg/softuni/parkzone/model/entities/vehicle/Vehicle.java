package bg.softuni.parkzone.model.entities.vehicle;

import bg.softuni.parkzone.model.entities.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EngineType engineType;

    @Column(nullable = false)
    private boolean disabledParkingRequired;

    @ManyToOne
    private User owner;

    @Column(nullable = false)
    private boolean active = true;



}
