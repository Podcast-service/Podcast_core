package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminAuthorProfileShortResponse(
        UUID authorId,
        String authorName,
        String description,
        long subscribersCount,
        OffsetDateTime createdAt
) {
}
