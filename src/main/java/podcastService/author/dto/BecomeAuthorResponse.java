package podcastService.author.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BecomeAuthorResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("expires_in")
        long expiresIn,
        @JsonProperty("author_profile")
        AuthorProfileResponse authorProfile
) {
}
