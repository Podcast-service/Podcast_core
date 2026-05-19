package podcastService.playlist.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.author.dto.AuthorCard;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.dto.CategoryResponse;
import podcastService.category.entity.CategoryEntity;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.playlist.dto.PlaylistDetailResponse;
import podcastService.playlist.dto.PlaylistOwnerResponse;
import podcastService.playlist.dto.PlaylistPodcastItem;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.entity.PlaylistPodcastEntity;
import podcastService.playlist.repository.PlaylistVoteRepository;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.vote.dto.VoteType;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlaylistMapper {

    private final PlaylistVoteRepository playlistVoteRepository;

    public PlaylistCard toCard(PlaylistEntity entity, UUID currentUserProfileId) {
        return new PlaylistCard(
                entity.getId(),
                entity.getTitle(),
                entity.getCoverImageUrl(),
                toOwner(entity),
                entity.isPublicPlaylist(),
                entity.getPodcastsCount(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getCreatedAt(),
                currentUserVote(entity.getId(), currentUserProfileId)
        );
    }

    public PlaylistDetailResponse toDetail(
            PlaylistEntity entity,
            List<PlaylistPodcastEntity> playlistPodcasts,
            UUID currentUserProfileId,
            UUID currentAuthorId
    ) {
        return new PlaylistDetailResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getCoverImageUrl(),
                toOwner(entity),
                entity.isPublicPlaylist(),
                (long) playlistPodcasts.size(),
                entity.getLikesCount(),
                entity.getDislikesCount(),
                entity.getCreatedAt(),
                currentUserVote(entity.getId(), currentUserProfileId),
                entity.getDescription(),
                entity.getUpdatedAt(),
                playlistPodcasts.stream()
                        .map(item -> toPodcastItem(item, currentAuthorId))
                        .toList()
        );
    }

    private PlaylistOwnerResponse toOwner(PlaylistEntity entity) {
        return new PlaylistOwnerResponse(
                entity.getOwner().getId(),
                entity.getOwner().getUsername()
        );
    }

    private PlaylistPodcastItem toPodcastItem(PlaylistPodcastEntity playlistPodcast, UUID currentAuthorId) {
        PodcastEntity podcast = playlistPodcast.getPodcast();
        return new PlaylistPodcastItem(
                podcast.getId(),
                podcast.getTitle(),
                toAuthorCard(podcast.getAuthor(), currentAuthorId),
                toCategoryResponse(podcast.getCategory()),
                podcast.getCoverImageUrl(),
                podcast.getDurationSeconds() == null ? null : podcast.getDurationSeconds().intValue(),
                podcast.getStatus(),
                podcast.getViewsCount(),
                podcast.getLikesCount(),
                podcast.getDislikesCount(),
                podcast.getPublishedAt(),
                podcast.getCreatedAt(),
                null,
                null,
                null,
                playlistPodcast.getPosition()
        );
    }

    private AuthorCard toAuthorCard(AuthorEntity author, UUID currentAuthorId) {
        return new AuthorCard(
                author.getId(),
                author.getAuthorName(),
                author.getUserProfile() == null ? null : author.getUserProfile().getAvatarUrl(),
                author.getSubscribersCount(),
                currentAuthorId != null && currentAuthorId.equals(author.getId())
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

    private VoteType currentUserVote(UUID playlistId, UUID currentUserProfileId) {
        if (currentUserProfileId == null) {
            return null;
        }

        return playlistVoteRepository.findByIdUserProfileIdAndIdPlaylistId(currentUserProfileId, playlistId)
                .map(vote -> vote.getVoteType())
                .orElse(null);
    }
}
