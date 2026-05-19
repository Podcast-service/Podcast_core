package podcastService.podcast.mapper;

import org.springframework.stereotype.Component;
import podcastService.author.dto.AuthorCard;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.dto.CategoryResponse;
import podcastService.category.entity.CategoryEntity;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.entity.PodcastEntity;

import java.util.UUID;

@Component
public class PodcastMapper {

    public PodcastCard toCard(PodcastEntity entity, UUID currentAuthorId) {
        return new PodcastCard(
                entity.getId(),
                entity.getTitle(),
                toAuthorCard(entity.getAuthor(), currentAuthorId),
                toCategoryResponse(entity.getCategory()),
                entity.getCoverImageUrl(),
                entity.getDurationSeconds() == null ? null : entity.getDurationSeconds().intValue(),
                entity.getStatus(),
                entity.getViewsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                null,
                null,
                null
        );
    }

    public PodcastDetailResponse toDetail(PodcastEntity entity, UUID currentAuthorId) {
        return new PodcastDetailResponse(
                entity.getId(),
                entity.getTitle(),
                toAuthorCard(entity.getAuthor(), currentAuthorId),
                toCategoryResponse(entity.getCategory()),
                entity.getCoverImageUrl(),
                entity.getDurationSeconds(),
                entity.getStatus(),
                entity.getViewsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                null,
                null,
                null,
                entity.getDescription(),
                entity.getAudioUrl(),
                false,
                false
        );
    }

    private AuthorCard toAuthorCard(AuthorEntity author, UUID currentAuthorId) {
        return new AuthorCard(
                author.getId(),
                author.getAuthorName(),
                author.getUserProfile() == null ? null : author.getUserProfile().getAvatarUrl(),
                author.getSubscribersCount(),
                null
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
