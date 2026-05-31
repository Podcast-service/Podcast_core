package podcastService.author.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AuthorFilter (
        String q,
        AuthorSort sort,
        @Min(1) Integer page,
        @Min(1) @Max(50) Integer size
) {
    public int normalizedPage() {
        return page == null ? 1 : page;
    }

    public int normalizedSize() {
        return size == null ? 20 : Math.min(size, 50);
    }

    public AuthorSort normalizedSort() {
        return sort == null ? AuthorSort.DATE_DESC : sort;
    }
};
