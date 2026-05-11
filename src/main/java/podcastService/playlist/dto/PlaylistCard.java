package podcastService.playlist.dto;

import podcastService.vote.dto.VoteType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PlaylistCard(
        UUID id,
        String title,
        String coverImageUrl,
        PlaylistOwnerResponse owner,
        Boolean isPublic,
        Long podcastsCount,
        Long likesCount,
        Long dislikesCount,
        OffsetDateTime createdAt,
        VoteType currentUserVote
) {
}
