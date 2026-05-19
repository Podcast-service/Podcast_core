package podcastService.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID userId,
        String username,
        String avatarUrl,
        OffsetDateTime createdAt
) {
}
