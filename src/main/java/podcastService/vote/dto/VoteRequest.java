package podcastService.vote.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull(message = "voteType must not be null")
        VoteType voteType
) {
}
