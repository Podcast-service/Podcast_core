package podcastService.user.dto;

import jakarta.validation.constraints.NotNull;

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
