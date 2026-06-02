package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPlaylistResponse(
        UUID id,
        String title,
        String description,
        String coverImageUrl,
        boolean isPublic,
        AdminUserProfileShortResponse owner,
        long podcastsCount,
        long likesCount,
        long dislikesCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
