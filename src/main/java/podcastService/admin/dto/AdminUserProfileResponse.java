package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserProfileResponse(
        UUID profileId,
        String username,
        String avatarUrl,
        String theme,
        String language,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
