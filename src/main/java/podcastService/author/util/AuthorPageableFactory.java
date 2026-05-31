package podcastService.author.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import podcastService.author.dto.AuthorSort;

import java.util.Objects;

public class AuthorPageableFactory {
    private AuthorPageableFactory() {}

    public static Pageable create(int page, int size, AuthorSort sort) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        AuthorSort normalizedSort = sort == null ? AuthorSort.DATE_DESC : sort;

        return PageRequest.of(
                normalizedPage - 1,
                normalizedSize,
                Objects.requireNonNull(mapSort(normalizedSort))
        );
    }

    private static Sort mapSort(AuthorSort sort) {
        return switch (sort) {
            case DATE_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case POPULAR, SUBSCRIBERS -> Sort.by(
                    Sort.Order.desc("subscribersCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}

