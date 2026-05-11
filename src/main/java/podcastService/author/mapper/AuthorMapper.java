package podcastService.author.mapper;

import org.springframework.stereotype.Component;
import podcastService.author.dto.AuthorCard;
import podcastService.author.dto.AuthorProfileResponse;
import podcastService.author.entity.AuthorEntity;

@Component
public class AuthorMapper {

    public AuthorProfileResponse toProfileResponse(AuthorEntity entity, Boolean isSubscribed) {
        return new AuthorProfileResponse(
                entity.getId(),
                entity.getUserProfile().getUserId(),
                entity.getAuthorName(),
                entity.getUserProfile().getAvatarUrl(),
                entity.getDescription(),
                entity.getSubscribersCount(),
                isSubscribed,
                entity.getCreatedAt()
        );
    }

    public AuthorCard toCard(AuthorEntity entity, Boolean isSubscribed) {
        return new AuthorCard(
                entity.getId(),
                entity.getAuthorName(),
                entity.getUserProfile() == null ? null : entity.getUserProfile().getAvatarUrl(),
                entity.getSubscribersCount(),
                isSubscribed
        );
    }
}
