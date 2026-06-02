package podcastService.admin.dto;

public record AdminUserFilter(
        String q,
        String role,
        Boolean emailVerified,
        int page,
        int size,
        AdminUserSort sort
) {
    public int normalizedPage() {
        return Math.max(page, 0);
    }

    public int normalizedSize() {
        return Math.min(Math.max(size, 1), 100);
    }

    public AdminUserSort normalizedSort() {
        return sort == null ? AdminUserSort.DATE_DESC : sort;
    }
}
