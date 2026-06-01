package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PlaylistDeletedPayload(
        UUID playlistId,
        UUID ownerUserId,
        Instant deletedAt,
        String status
) {
}
