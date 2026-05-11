package podcastService.playlist.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddPodcastToPlaylistRequest(
        @NotNull(message = "podcastId must not be null")
        UUID podcastId
) {
}
