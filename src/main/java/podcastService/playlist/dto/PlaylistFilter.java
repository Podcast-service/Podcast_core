package podcastService.playlist.dto;

public record PlaylistFilter(
        String q,
        SortPlaylists sort,
        int page,
        int size
) {
    public int normalizedPage() {
        return Math.max(page, 1);
    }

    public int normalizedSize() {
        return Math.min(Math.max(size, 1), 50);
    }

    public SortPlaylists normalizedSort() {
        return sort == null ? SortPlaylists.DATE_DESC : sort;
    }
}
