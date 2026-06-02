package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPlaylistItemResponse(
        UUID podcastId,
        int position,
        String title,
        String status,
        String authorName,
        OffsetDateTime addedAt
) {
}
