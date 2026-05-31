package podcastService.author.specifications;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import podcastService.author.entity.AuthorEntity;

public final class AuthorSpecifications {
    private AuthorSpecifications() {}

    public static Specification<AuthorEntity> searchByText(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return noOp();
        }

        String normalized = "%" + queryText.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("authorName")), normalized),
                cb.like(cb.lower(root.get("description")), normalized)
        );
    }

    public static Specification<AuthorEntity> fetchUserProfile() {
        return (root, query, cb) -> {
            if (query.getResultType() != null
                    && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                root.fetch("userProfile", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    private static Specification<AuthorEntity> noOp() {
        return (root, query, cb) -> cb.conjunction();
    }
}
