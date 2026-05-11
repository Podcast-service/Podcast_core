package podcastService.vote.dto;

import java.util.UUID;

public record VoteResponse(
        UUID targetId,
        String targetType,
        Long likesCount,
        Long dislikesCount,
        VoteType currentUserVote
) {
}
