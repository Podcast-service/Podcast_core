package podcastService.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID userId,
        String email,
        List<String> roles,
        boolean emailVerified,
        OffsetDateTime authCreatedAt,
        AdminUserProfileResponse profile,
        AdminAuthorProfileShortResponse authorProfile
) {
}
