package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPodcastResponse(
        UUID id,
        String title,
        String description,
        AdminPodcastAuthorResponse author,
        AdminCategoryShortResponse category,
        String coverImageUrl,
        String audioUrl,
        Integer durationSeconds,
        Integer numSpeakers,
        String status,
        long viewsCount,
        long likesCount,
        long dislikesCount,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean hasTranscript,
        boolean hasSummary
) {
}
