package podcastService.playlist.dto;

import java.util.UUID;

public record PlaylistOwnerResponse(
        UUID id,
        String username
) {
}
