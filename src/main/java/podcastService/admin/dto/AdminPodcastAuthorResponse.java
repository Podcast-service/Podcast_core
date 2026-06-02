package podcastService.admin.dto;

import java.util.UUID;

public record AdminPodcastAuthorResponse(
        UUID id,
        UUID userProfileId,
        String authorName,
        String avatarUrl
) {
}
