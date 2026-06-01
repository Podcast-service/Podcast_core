package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PlaylistCreatedPayload(
        UUID playlistId,
        UUID ownerUserId,
        String title,
        boolean publicPlaylist,
        Instant createdAt
) {
}
