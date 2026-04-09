package podcastService.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username must match pattern ^[a-zA-Z0-9_]+$")
        String username,

        String avatarUrl
) {
}
