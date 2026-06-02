package podcastService.admin.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthAdminUserResponse(
        @JsonAlias("user_id")
        UUID userId,
        String email,
        List<String> roles,
        @JsonAlias("email_verified")
        boolean emailVerified,
        @JsonAlias({"auth_created_at", "created_at"})
        OffsetDateTime authCreatedAt
) {
}
