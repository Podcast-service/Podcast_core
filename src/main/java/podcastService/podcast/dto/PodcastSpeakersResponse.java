package podcastService.podcast.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record PodcastSpeakersResponse(
        UUID podcastId,
        @JsonProperty("num_speakers")
        Integer numSpeakers
) {
}
