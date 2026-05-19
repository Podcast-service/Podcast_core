package podcastService.podcast.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import podcastService.podcast.dto.SortPodcasts;

public final class PodcastPageableFactory {

    private PodcastPageableFactory() {
    }

    public static Pageable create(int page, int size, SortPodcasts sort) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        SortPodcasts normalizedSort = sort == null ? SortPodcasts.DATE_DESC : sort;

        return PageRequest.of(
                normalizedPage - 1,
                normalizedSize,
                mapSort(normalizedSort)
        );
    }

    private static Sort mapSort(SortPodcasts sort) {
        return switch (sort) {
            case DATE_ASC -> Sort.by(
                    Sort.Order.asc("publishedAt").nullsLast(),
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
            case RATING -> Sort.by(
                    Sort.Order.desc("likesCount"),
                    Sort.Order.asc("dislikesCount"),
                    Sort.Order.desc("viewsCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case VIEWS -> Sort.by(
                    Sort.Order.desc("viewsCount"),
                    Sort.Order.desc("likesCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case DATE_DESC -> Sort.by(
                    Sort.Order.desc("publishedAt").nullsLast(),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
