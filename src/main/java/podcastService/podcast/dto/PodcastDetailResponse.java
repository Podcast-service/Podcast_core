package podcastService.podcast.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import podcastService.author.dto.AuthorCard;
import podcastService.category.dto.CategoryResponse;
import podcastService.podcast.entity.Status;
import podcastService.vote.dto.VoteType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodcastDetailResponse(
        UUID id,
        String title,
        AuthorCard author,
        CategoryResponse category,
        String coverImageUrl,
        Long durationSeconds,
        @JsonProperty("num_speakers")
        Integer numSpeakers,
        Status status,
        Long viewsCount,
        Long likesCount,
        Long dislikesCount,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        VoteType currentUserVote,
        Long progressSeconds,
        Integer progressPercent,
        String description,
        String audioUrl,
        @JsonProperty("audio_url_file")
        String audioUrlFile,
        @JsonProperty("audio_size_file")
        Long audioSizeFile,
        Boolean hasTranscript,
        Boolean hasSummary
) {
}
