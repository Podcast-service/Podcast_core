package podcastService.author.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthorProfileResponse(
        UUID id,
        UUID userId,
        String authorName,
        String avatarUrl,
        String description,
        long subscribersCount,
        Boolean isSubscribed,
        OffsetDateTime createdAt
) {
}
