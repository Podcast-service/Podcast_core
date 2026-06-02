package podcastService.admin.dto;

import podcastService.playlist.dto.SortPlaylists;

import java.util.UUID;

public record AdminPlaylistFilter(
        String q,
        UUID ownerProfileId,
        Boolean isPublic,
        int page,
        int size,
        SortPlaylists sort
) {
    public int normalizedPage() {
        return Math.max(page, 0);
    }

    public int normalizedSize() {
        return Math.min(Math.max(size, 1), 100);
    }

    public SortPlaylists normalizedSort() {
        return sort == null ? SortPlaylists.DATE_DESC : sort;
    }
}
