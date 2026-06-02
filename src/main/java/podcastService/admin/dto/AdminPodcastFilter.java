package podcastService.admin.dto;

import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.entity.Status;

import java.util.UUID;

public record AdminPodcastFilter(
        String q,
        Status status,
        UUID authorId,
        UUID categoryId,
        int page,
        int size,
        SortPodcasts sort
) {
    public int normalizedPage() {
        return Math.max(page, 0);
    }

    public int normalizedSize() {
        return Math.min(Math.max(size, 1), 100);
    }

    public SortPodcasts normalizedSort() {
        return sort == null ? SortPodcasts.DATE_DESC : sort;
    }
}
