package podcastService.user.dto;

import podcastService.user.entity.Language;
import podcastService.user.entity.Theme;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfilePrivateResponse(
        UUID id,
        UUID userId,
        String username,
        String avatarUrl,
        Theme theme,
        Language language,
        OffsetDateTime createdAt
) {
}
