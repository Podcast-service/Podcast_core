package podcastService.podcast.dto;

import podcastService.author.dto.AuthorCard;
import podcastService.category.dto.CategoryResponse;
import podcastService.podcast.entity.Status;
import podcastService.vote.dto.VoteType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodcastCard(
        UUID id,
        String title,
        AuthorCard author,
        CategoryResponse category,
        String coverImageUrl,
        Integer durationSeconds,
        Status status,
        Long viewsCount,
        Long likesCount,
        Long dislikesCount,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        VoteType currentUserVote,
        Long progressSeconds,
        Integer progressPercent
) {
}
