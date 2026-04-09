package podcastService.user.dto;

import java.util.UUID;

public record CreateUserRequest (
        UUID userId,
        String username
) {
}
