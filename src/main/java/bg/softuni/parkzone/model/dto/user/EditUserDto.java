package bg.softuni.parkzone.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditUserDto {

    @Size(min = 2, max = 20, message = "First name must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Must contain only letters")
    private String firstName;

    @Size(min = 2, max = 20, message = "Last name must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Must contain only letters")
    private String lastName;

    @Email(message = "Email should be valid")
    private String email;

    @Pattern(regexp = "^(\\+359|0)[0-9]{9}$", message = "Phone number must start with +359 or 0 and contain 10 digits")
    private String phoneNumber;


}
