package podcastService.playlist.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import podcastService.playlist.dto.SortPlaylists;

public final class PlaylistPageableFactory {

    private PlaylistPageableFactory() {
    }

    public static Pageable create(int page, int size, SortPlaylists sort) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        SortPlaylists normalizedSort = sort == null ? SortPlaylists.DATE_DESC : sort;

        return PageRequest.of(normalizedPage - 1, normalizedSize, mapSort(normalizedSort));
    }

    private static Sort mapSort(SortPlaylists sort) {
        return switch (sort) {
            case DATE_ASC -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
            case RATING -> Sort.by(
                    Sort.Order.desc("likesCount"),
                    Sort.Order.asc("dislikesCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case DATE_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
