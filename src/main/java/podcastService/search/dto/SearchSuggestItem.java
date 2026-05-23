package podcastService.search.dto;

import java.util.UUID;

public record SearchSuggestItem(
        SearchType type,
        UUID id,
        String label,
        String coverUrl
) {
}
