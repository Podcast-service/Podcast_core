package podcastService.admin.dto;

import java.util.UUID;

public record AdminCategoryShortResponse(
        UUID id,
        String name,
        int position
) {
}
