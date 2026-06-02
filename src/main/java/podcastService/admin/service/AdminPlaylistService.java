package podcastService.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminPlaylistDetailResponse;
import podcastService.admin.dto.AdminPlaylistFilter;
import podcastService.admin.dto.AdminPlaylistResponse;
import podcastService.admin.mapper.AdminMapper;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.outbox.recommendation.PlaylistContentEventFactory;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.playlist.dto.SortPlaylists;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.entity.PlaylistPodcastEntity;
import podcastService.playlist.repository.PlaylistPodcastRepository;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.playlist.specifications.PlaylistSpecifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistPodcastRepository playlistPodcastRepository;
    private final RecommendationOutboxEventService recommendationOutboxEventService;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminPlaylistResponse> getPlaylists(AdminPlaylistFilter filter) {
        Specification<PlaylistEntity> specification = Specification
                .where(PlaylistSpecifications.fetchOwner())
                .and(PlaylistSpecifications.searchByTitle(filter.q()))
                .and(PlaylistSpecifications.withOwnerProfileId(filter.ownerProfileId()))
                .and(PlaylistSpecifications.withPublicStatus(filter.isPublic()));

        Page<PlaylistEntity> page = playlistRepository.findAll(
                specification,
                PageRequest.of(filter.normalizedPage(), filter.normalizedSize(), sort(filter.normalizedSort()))
        );

        return new AdminPageResponse<>(
                page.map(adminMapper::toPlaylistResponse).getContent(),
                filter.normalizedPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminPlaylistDetailResponse getPlaylist(UUID playlistId) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerById(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));
        List<PlaylistPodcastEntity> items = playlistPodcastRepository.findByIdPlaylistIdOrderByPositionAsc(playlistId);

        return adminMapper.toPlaylistDetailResponse(playlist, items);
    }

    @Transactional
    public void deletePlaylist(UUID playlistId, UUID currentAdminUserId) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        playlistRepository.delete(playlist);
        playlistRepository.flush();
        recommendationOutboxEventService.savePlaylistEvent(
                playlistId,
                PlaylistContentEventFactory.deleted(
                        playlistId,
                        currentAdminUserId,
                        Instant.now(),
                        null,
                        null
                )
        );

        log.info("Playlist deleted by admin: playlistId={}, adminUserId={}, ownerProfileId={}",
                playlistId, currentAdminUserId, playlist.getOwner().getId());
    }

    private Sort sort(SortPlaylists sort) {
        return switch (sort) {
            case DATE_ASC -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
            case RATING -> Sort.by(
                    Sort.Order.desc("likesCount"),
                    Sort.Order.asc("dislikesCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case DATE_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }
}
