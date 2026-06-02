package podcastService.playlist.specifications;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import podcastService.playlist.entity.PlaylistEntity;

import java.util.UUID;

public final class PlaylistSpecifications {

    private PlaylistSpecifications() {
    }

    public static Specification<PlaylistEntity> fetchOwner() {
        return (root, query, cb) -> {
            if (query.getResultType() != null
                    && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                root.fetch("owner", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    public static Specification<PlaylistEntity> withPublicOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("publicPlaylist"));
    }

    public static Specification<PlaylistEntity> withPublicStatus(Boolean isPublic) {
        if (isPublic == null) {
            return noOp();
        }
        return (root, query, cb) -> isPublic
                ? cb.isTrue(root.get("publicPlaylist"))
                : cb.isFalse(root.get("publicPlaylist"));
    }

    public static Specification<PlaylistEntity> withOwnerUserId(UUID userId) {
        if (userId == null) {
            return noOp();
        }
        return (root, query, cb) -> cb.equal(root.get("owner").get("userId"), userId);
    }

    public static Specification<PlaylistEntity> withOwnerProfileId(UUID profileId) {
        if (profileId == null) {
            return noOp();
        }
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), profileId);
    }

    public static Specification<PlaylistEntity> searchByTitle(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return noOp();
        }

        String normalized = "%" + queryText.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), normalized);
    }

    private static Specification<PlaylistEntity> noOp() {
        return (root, query, cb) -> cb.conjunction();
    }
}
