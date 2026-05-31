package podcastService.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthRoleUpdateRequest(
        @JsonProperty("role_name")
        String roleName
) {
}
