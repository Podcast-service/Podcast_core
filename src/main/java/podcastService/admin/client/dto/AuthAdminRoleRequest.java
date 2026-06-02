package podcastService.admin.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthAdminRoleRequest(
        @JsonProperty("role_name")
        String roleName
) {
}
