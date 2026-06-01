package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PlaylistUpdatedPayload(
        UUID playlistId,
        UUID ownerUserId,
        String title,
        boolean publicPlaylist,
        Instant updatedAt
) {
}
