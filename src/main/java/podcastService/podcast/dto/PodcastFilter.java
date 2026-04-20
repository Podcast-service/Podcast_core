package podcastService.podcast.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record PodcastFilter(
        String q,
        UUID categoryId,
        UUID authorId,
        SortPodcasts sort,
        @Min(1) Integer page,
        @Min(1) @Max(50) Integer size
) {
    public int normalizedPage() {
        return page == null ? 1 : page;
    }

    public int normalizedSize() {
        return size == null ? 20 : Math.min(size, 50);
    }

    public SortPodcasts normalizedSort() {
        return sort == null ? SortPodcasts.DATE_DESC : sort;
    }
}
