package bg.softuni.parkzone.model.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserProfileUpdateRequestDTO {

    private String username;

    private String email;

    @Size(max = 20, message = "First name must be up to 20 characters")
    @Pattern(regexp = "$|^[A-Za-z]+$", message = "First name must contain only letters")
    private String firstName;

    @Size(max = 20, message = "Last name must be up to 20 characters")
    @Pattern(regexp = "$|^[A-Za-z]+$", message = "Last name must contain only letters")
    private String lastName;

    @Pattern(regexp = "$|^((0)[0-9]{9})$", message = "Phone number must start with 0 and contain 10 digits")
    private String phoneNumber;

}
