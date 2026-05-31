package podcastService.author.service;

import podcastService.author.dto.AuthorProfileResponse;

public record AuthorProfileCreationResult(
        AuthorProfileResponse profile,
        boolean created
) {
}
