package bg.softuni.parkzone.model.entities.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    private String firstName;

    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Pattern(regexp = "^(\\+359|0)[0-9]{9}$", message = "Phone number must start with +359 or 0 and contain 10 digits")
    private String phoneNumber;

    @Column(nullable = false)
    private boolean isActive;






}
