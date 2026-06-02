package podcastService.admin.mapper;

import org.springframework.stereotype.Component;
import podcastService.admin.client.dto.AuthAdminRoleMutationResponse;
import podcastService.admin.client.dto.AuthAdminUserResponse;
import podcastService.admin.dto.AdminAuthorProfileShortResponse;
import podcastService.admin.dto.AdminCategoryShortResponse;
import podcastService.admin.dto.AdminPlaylistDetailResponse;
import podcastService.admin.dto.AdminPlaylistItemResponse;
import podcastService.admin.dto.AdminPlaylistResponse;
import podcastService.admin.dto.AdminPodcastAuthorResponse;
import podcastService.admin.dto.AdminPodcastResponse;
import podcastService.admin.dto.AdminRoleMutationResponse;
import podcastService.admin.dto.AdminUserProfileResponse;
import podcastService.admin.dto.AdminUserProfileShortResponse;
import podcastService.admin.dto.AdminUserResponse;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.entity.CategoryEntity;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.entity.PlaylistPodcastEntity;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.user.entity.UserProfileEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class AdminMapper {

    public AdminPodcastResponse toPodcastResponse(
            PodcastEntity entity,
            Set<UUID> podcastIdsWithTranscripts,
            Set<UUID> podcastIdsWithSummaries
    ) {
        return new AdminPodcastResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                toPodcastAuthor(entity.getAuthor()),
                toCategory(entity.getCategory()),
                entity.getCoverImageUrl(),
                entity.getAudioUrl(),
                entity.getDurationSeconds() == null ? null : entity.getDurationSeconds().intValue(),
                entity.getNumSpeakers(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getViewsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                podcastIdsWithTranscripts.contains(entity.getId()),
                podcastIdsWithSummaries.contains(entity.getId())
        );
    }

    public AdminPlaylistResponse toPlaylistResponse(PlaylistEntity entity) {
        return new AdminPlaylistResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCoverImageUrl(),
                entity.isPublicPlaylist(),
                toUserProfileShort(entity.getOwner()),
                entity.getPodcastsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AdminPlaylistDetailResponse toPlaylistDetailResponse(
            PlaylistEntity entity,
            List<PlaylistPodcastEntity> items
    ) {
        return new AdminPlaylistDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCoverImageUrl(),
                entity.isPublicPlaylist(),
                toUserProfileShort(entity.getOwner()),
                items.stream().map(this::toPlaylistItem).toList(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AdminUserResponse toUserResponse(
            AuthAdminUserResponse authUser,
            Map<UUID, UserProfileEntity> profilesByUserId,
            Map<UUID, AuthorEntity> authorsByUserId
    ) {
        UserProfileEntity profile = profilesByUserId.get(authUser.userId());
        AuthorEntity author = authorsByUserId.get(authUser.userId());
        return new AdminUserResponse(
                authUser.userId(),
                authUser.email(),
                authUser.roles() == null ? List.of() : authUser.roles(),
                authUser.emailVerified(),
                authUser.authCreatedAt(),
                toUserProfile(profile),
                toAuthorProfile(author)
        );
    }

    public AdminRoleMutationResponse toRoleMutationResponse(AuthAdminRoleMutationResponse response) {
        return new AdminRoleMutationResponse(
                response.userId(),
                response.roles(),
                response.changed()
        );
    }

    private AdminPodcastAuthorResponse toPodcastAuthor(AuthorEntity author) {
        UserProfileEntity profile = author.getUserProfile();
        return new AdminPodcastAuthorResponse(
                author.getId(),
                profile == null ? null : profile.getId(),
                author.getAuthorName(),
                profile == null ? null : profile.getAvatarUrl()
        );
    }

    private AdminCategoryShortResponse toCategory(CategoryEntity category) {
        if (category == null) {
            return null;
        }
        return new AdminCategoryShortResponse(
                category.getId(),
                category.getName(),
                category.getPosition()
        );
    }

    private AdminUserProfileShortResponse toUserProfileShort(UserProfileEntity profile) {
        if (profile == null) {
            return null;
        }
        return new AdminUserProfileShortResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getUsername(),
                profile.getAvatarUrl()
        );
    }

    private AdminPlaylistItemResponse toPlaylistItem(PlaylistPodcastEntity item) {
        PodcastEntity podcast = item.getPodcast();
        return new AdminPlaylistItemResponse(
                podcast.getId(),
                item.getPosition(),
                podcast.getTitle(),
                podcast.getStatus() == null ? null : podcast.getStatus().name(),
                podcast.getAuthor() == null ? null : podcast.getAuthor().getAuthorName(),
                item.getAddedAt()
        );
    }

    private AdminUserProfileResponse toUserProfile(UserProfileEntity profile) {
        if (profile == null) {
            return null;
        }
        return new AdminUserProfileResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getAvatarUrl(),
                profile.getTheme() == null ? null : profile.getTheme().name(),
                profile.getLanguage() == null ? null : profile.getLanguage().name(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private AdminAuthorProfileShortResponse toAuthorProfile(AuthorEntity author) {
        if (author == null) {
            return null;
        }
        return new AdminAuthorProfileShortResponse(
                author.getId(),
                author.getAuthorName(),
                author.getDescription(),
                author.getSubscribersCount(),
                author.getCreatedAt()
        );
    }
}
