package podcastService.podcast.mapper;

import org.springframework.stereotype.Component;
import podcastService.author.dto.AuthorCard;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.dto.CategoryResponse;
import podcastService.category.entity.CategoryEntity;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.vote.dto.VoteType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class PodcastMapper {

    public PodcastCard toCard(PodcastEntity entity, UUID currentAuthorId) {
        return toCard(entity, currentAuthorId, null);
    }

    public PodcastCard toCard(PodcastEntity entity, UUID currentAuthorId, Set<UUID> subscribedAuthorIds) {
        return toCard(entity, currentAuthorId, subscribedAuthorIds, null);
    }

    public PodcastCard toCard(
            PodcastEntity entity,
            UUID currentAuthorId,
            Set<UUID> subscribedAuthorIds,
            Map<UUID, VoteType> currentUserVotes
    ) {
        return new PodcastCard(
                entity.getId(),
                entity.getTitle(),
                toAuthorCard(entity.getAuthor(), subscribedAuthorIds),
                toCategoryResponse(entity.getCategory()),
                entity.getCoverImageUrl(),
                entity.getDurationSeconds() == null ? null : entity.getDurationSeconds().intValue(),
                entity.getStatus(),
                entity.getViewsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                currentUserVotes == null ? null : currentUserVotes.get(entity.getId()),
                null,
                null
        );
    }

    public PodcastDetailResponse toDetail(PodcastEntity entity, UUID currentAuthorId) {
        return toDetail(entity, currentAuthorId, null);
    }

    public PodcastDetailResponse toDetail(PodcastEntity entity, UUID currentAuthorId, Set<UUID> subscribedAuthorIds) {
        return toDetail(entity, currentAuthorId, subscribedAuthorIds, null, false, false);
    }

    public PodcastDetailResponse toDetail(
            PodcastEntity entity,
            UUID currentAuthorId,
            Set<UUID> subscribedAuthorIds,
            VoteType currentUserVote,
            boolean hasTranscript,
            boolean hasSummary
    ) {
        return new PodcastDetailResponse(
                entity.getId(),
                entity.getTitle(),
                toAuthorCard(entity.getAuthor(), subscribedAuthorIds),
                toCategoryResponse(entity.getCategory()),
                entity.getCoverImageUrl(),
                entity.getDurationSeconds(),
                entity.getStatus(),
                entity.getViewsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                currentUserVote,
                null,
                null,
                entity.getDescription(),
                entity.getAudioUrl(),
                hasTranscript,
                hasSummary
        );
    }

    private AuthorCard toAuthorCard(AuthorEntity author, Set<UUID> subscribedAuthorIds) {
        return new AuthorCard(
                author.getId(),
                author.getAuthorName(),
                author.getUserProfile() == null ? null : author.getUserProfile().getAvatarUrl(),
                author.getSubscribersCount(),
                subscribedAuthorIds == null ? null : subscribedAuthorIds.contains(author.getId())
        );
    }

    private CategoryResponse toCategoryResponse(CategoryEntity category) {
        if (category == null) {
            return null;
        }

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getPosition()
        );
    }
}
