package podcastService.playlist.dto;

import podcastService.vote.dto.VoteType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PlaylistDetailResponse(
        UUID id,
        String title,
        String coverImageUrl,
        PlaylistOwnerResponse owner,
        Boolean isPublic,
        Long podcastsCount,
        Long likesCount,
        Long dislikesCount,
        OffsetDateTime createdAt,
        VoteType currentUserVote,
        String description,
        OffsetDateTime updatedAt,
        List<PlaylistPodcastItem> podcasts
) {
}
