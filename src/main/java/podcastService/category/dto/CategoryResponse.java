package podcastService.category.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        Integer position
) {
}
