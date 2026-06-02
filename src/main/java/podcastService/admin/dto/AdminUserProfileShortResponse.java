package podcastService.admin.dto;

import java.util.UUID;

public record AdminUserProfileShortResponse(
        UUID profileId,
        UUID userId,
        String username,
        String avatarUrl
) {
}
