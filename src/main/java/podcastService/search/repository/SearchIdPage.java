package podcastService.search.repository;

import java.util.List;
import java.util.UUID;

public record SearchIdPage(
        List<UUID> ids,
        long totalElements
) {
}
