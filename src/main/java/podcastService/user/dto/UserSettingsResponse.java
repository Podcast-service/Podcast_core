package podcastService.user.dto;

import podcastService.user.entity.Language;
import podcastService.user.entity.Theme;

public record UserSettingsResponse(
        Theme theme,
        Language language
) {
}
