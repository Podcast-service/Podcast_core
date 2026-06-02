package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistUpdatedPayload(
        UUID playlistId,
        UUID ownerUserId,
        String title,
        String description,
        boolean publicPlaylist,
        List<UUID> podcastIds,
        Instant createdAt,
        Instant updatedAt
) {
}
