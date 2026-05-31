package podcastService.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthRoleUpdateResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("expires_in")
        long expiresIn
) {
}
