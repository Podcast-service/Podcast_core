package podcastService.admin.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthAdminRoleMutationResponse(
        @JsonAlias("user_id")
        UUID userId,
        List<String> roles,
        boolean changed
) {
}
