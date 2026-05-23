package podcastService.history.dto;

import podcastService.podcast.dto.PodcastCard;

import java.time.OffsetDateTime;

public record ListenHistoryItem(
        PodcastCard podcast,
        Integer progressSeconds,
        Integer progressPercent,
        Boolean completed,
        OffsetDateTime lastListenedAt
) {
}
