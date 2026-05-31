package podcastService.playlist.dto;

import java.util.UUID;

public record PlaylistSaveResponse(
        UUID playlistId,
        boolean isSaved
) {
}
