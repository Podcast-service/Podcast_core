package podcastService.author.service;

import podcastService.author.dto.BecomeAuthorResponse;

public record BecomeAuthorResult(
        BecomeAuthorResponse response,
        boolean authorProfileCreated
) {
}
