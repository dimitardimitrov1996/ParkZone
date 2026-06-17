package bg.softuni.parkzone.model.dto.user;

import bg.softuni.parkzone.model.entities.user.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserDto {

    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserRole role;
    private String phoneNumber;
    private boolean isActive;

}
