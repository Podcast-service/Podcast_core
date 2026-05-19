package podcastService.podcast.specifications;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;

import java.util.UUID;

public final class PodcastSpecifications {

    private PodcastSpecifications() {}

    public static Specification<PodcastEntity> fetchRelations() {
        return (root, query, cb) -> {
            if (query.getResultType() != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("author", JoinType.LEFT).fetch("userProfile", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    public static Specification<PodcastEntity> withPublishedOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), Status.PUBLISHED);
    }

    public static Specification<PodcastEntity> withAuthorId(UUID authorId) {
        if (authorId == null) {
            return noOp();
        }

        return (root, query, cb) -> cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<PodcastEntity> withCategoryId(UUID categoryId) {
        if (categoryId == null) {
            return noOp();
        }

        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<PodcastEntity> searchByText(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return noOp();
        }

        String normalized = "%" + queryText.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), normalized),
                cb.like(cb.lower(root.get("description")), normalized)
        );
    }

    private static Specification<PodcastEntity> noOp() {
        return (root, query, cb) -> cb.conjunction();
    }
}
