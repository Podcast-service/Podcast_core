package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminPlaylistDetailResponse(
        UUID id,
        String title,
        String description,
        String coverImageUrl,
        boolean isPublic,
        AdminUserProfileShortResponse owner,
        List<AdminPlaylistItemResponse> items,
        long likesCount,
        long dislikesCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
