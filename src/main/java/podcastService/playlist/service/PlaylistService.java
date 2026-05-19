package podcastService.playlist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.BusinessRuleException;
import podcastService.common.exception.ConflictException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.playlist.dto.AddPodcastToPlaylistRequest;
import podcastService.playlist.dto.CreatePlaylistRequest;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.playlist.dto.PlaylistDetailResponse;
import podcastService.playlist.dto.PlaylistFilter;
import podcastService.playlist.dto.ReorderPlaylistRequest;
import podcastService.playlist.dto.SortPlaylists;
import podcastService.playlist.dto.UpdatePlaylistRequest;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.entity.PlaylistPodcastEntity;
import podcastService.playlist.entity.PlaylistPodcastId;
import podcastService.playlist.entity.PlaylistVoteEntity;
import podcastService.playlist.entity.PlaylistVoteId;
import podcastService.playlist.mapper.PlaylistMapper;
import podcastService.playlist.repository.PlaylistPodcastRepository;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.playlist.repository.PlaylistVoteRepository;
import podcastService.playlist.specifications.PlaylistSpecifications;
import podcastService.playlist.util.PlaylistPageableFactory;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteRequest;
import podcastService.vote.dto.VoteResponse;
import podcastService.vote.dto.VoteType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final int MAX_PLAYLIST_SIZE = 500;

    private final PlaylistRepository playlistRepository;
    private final PlaylistPodcastRepository playlistPodcastRepository;
    private final PlaylistVoteRepository playlistVoteRepository;
    private final PodcastRepository podcastRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorRepository authorRepository;
    private final PlaylistMapper playlistMapper;

    @Transactional(readOnly = true)
    public PageResponse<PlaylistCard> listPublic(PlaylistFilter filter, UUID currentUserId) {
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);

        Specification<PlaylistEntity> specification = Specification
                .where(PlaylistSpecifications.fetchOwner())
                .and(PlaylistSpecifications.withPublicOnly())
                .and(PlaylistSpecifications.searchByTitle(filter.q()));

        return mapPage(
                playlistRepository.findAll(
                        specification,
                        PlaylistPageableFactory.create(
                                filter.normalizedPage(),
                                filter.normalizedSize(),
                                filter.normalizedSort()
                        )
                ),
                filter.normalizedPage(),
                currentUserProfileId
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistCard> listMine(UUID currentUserId, int page, int size) {
        UUID currentUserProfileId = requireUserProfile(currentUserId).getId();

        Specification<PlaylistEntity> specification = Specification
                .where(PlaylistSpecifications.fetchOwner())
                .and(PlaylistSpecifications.withOwnerUserId(currentUserId));

        return mapPage(
                playlistRepository.findAll(
                        specification,
                        PlaylistPageableFactory.create(page, size, SortPlaylists.DATE_DESC)
                ),
                Math.max(page, 1),
                currentUserProfileId
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistCard> listAuthorPlaylists(UUID authorId, int page, int size, UUID currentUserId) {
        AuthorEntity author = authorRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        UUID currentUserProfileId = resolveUserProfileId(currentUserId);

        Specification<PlaylistEntity> specification = Specification
                .where(PlaylistSpecifications.fetchOwner())
                .and(PlaylistSpecifications.withPublicOnly())
                .and(PlaylistSpecifications.withOwnerProfileId(author.getUserProfile().getId()));

        return mapPage(
                playlistRepository.findAll(
                        specification,
                        PlaylistPageableFactory.create(page, size, SortPlaylists.DATE_DESC)
                ),
                Math.max(page, 1),
                currentUserProfileId
        );
    }

    @Transactional
    public PlaylistDetailResponse create(UUID currentUserId, CreatePlaylistRequest request) {
        UserProfileEntity owner = requireUserProfile(currentUserId);

        PlaylistEntity entity = new PlaylistEntity();
        entity.setOwner(owner);
        entity.setTitle(normalizeRequiredText(request.title(), "title"));
        entity.setDescription(normalizeNullableText(request.description()));
        entity.setCoverImageUrl(normalizeUri(request.coverImageUrl(), "coverImageUrl"));
        entity.setPublicPlaylist(Boolean.TRUE.equals(request.isPublic()));

        PlaylistEntity saved = playlistRepository.saveAndFlush(entity);

        log.info(
                "Playlist created: playlistId={}, ownerProfileId={}, userId={}, public={}",
                saved.getId(),
                owner.getId(),
                currentUserId,
                saved.isPublicPlaylist()
        );

        return playlistMapper.toDetail(saved, List.of(), owner.getId(), resolveAuthorIdByUserId(currentUserId));
    }

    @Transactional(readOnly = true)
    public PlaylistDetailResponse get(UUID playlistId, UUID currentUserId) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerById(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        UUID currentUserProfileId = resolveUserProfileId(currentUserId);
        ensureVisible(playlist, currentUserProfileId);

        List<PlaylistPodcastEntity> items = playlistPodcastRepository.findByIdPlaylistIdOrderByPositionAsc(playlistId);

        return playlistMapper.toDetail(
                playlist,
                items,
                currentUserProfileId,
                resolveAuthorIdByUserId(currentUserId)
        );
    }

    @Transactional
    public PlaylistDetailResponse update(UUID playlistId, UUID currentUserId, UpdatePlaylistRequest request) {
        validateUpdateRequest(request);

        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        ensureOwner(playlist, currentUser.getId());

        if (request.isTitleSet()) {
            playlist.setTitle(normalizeRequiredText(request.getTitle(), "title"));
        }
        if (request.isDescriptionSet()) {
            playlist.setDescription(normalizeNullableText(request.getDescription()));
        }
        if (request.isCoverImageUrlSet()) {
            playlist.setCoverImageUrl(normalizeUri(request.getCoverImageUrl(), "coverImageUrl"));
        }
        if (request.isPublicSet()) {
            playlist.setPublicPlaylist(Boolean.TRUE.equals(request.getIsPublic()));
        }

        PlaylistEntity saved = playlistRepository.saveAndFlush(playlist);
        List<PlaylistPodcastEntity> items = playlistPodcastRepository.findByIdPlaylistIdOrderByPositionAsc(playlistId);

        log.info(
                "Playlist updated: playlistId={}, userId={}, titleChanged={}, descriptionChanged={}, coverChanged={}, publicChanged={}",
                playlistId,
                currentUserId,
                request.isTitleSet(),
                request.isDescriptionSet(),
                request.isCoverImageUrlSet(),
                request.isPublicSet()
        );

        return playlistMapper.toDetail(saved, items, currentUser.getId(), resolveAuthorIdByUserId(currentUserId));
    }

    @Transactional
    public void delete(UUID playlistId, UUID currentUserId) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        ensureOwner(playlist, currentUser.getId());

        playlistRepository.delete(playlist);
        playlistRepository.flush();

        log.info("Playlist deleted: playlistId={}, userId={}", playlistId, currentUserId);
    }

    @Transactional
    public PlaylistDetailResponse addPodcast(UUID playlistId, UUID currentUserId, AddPodcastToPlaylistRequest request) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        ensureOwner(playlist, currentUser.getId());

        PodcastEntity podcast = podcastRepository.findDetailedById(request.podcastId())
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        if (podcast.getStatus() != Status.PUBLISHED) {
            throw new BusinessRuleException("Only published podcasts can be added to playlist");
        }

        if (playlistPodcastRepository.existsByIdPlaylistIdAndIdPodcastId(playlistId, request.podcastId())) {
            throw new ConflictException("Podcast already exists in playlist");
        }

        int maxPosition = playlistPodcastRepository.findMaxPosition(playlistId);
        if (maxPosition >= MAX_PLAYLIST_SIZE) {
            throw new BusinessRuleException("Playlist size limit exceeded");
        }

        PlaylistPodcastEntity item = new PlaylistPodcastEntity();
        item.setId(new PlaylistPodcastId(playlistId, request.podcastId()));
        item.setPlaylist(playlist);
        item.setPodcast(podcast);
        item.setPosition(maxPosition + 1);

        try {
            playlistPodcastRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "Failed to add podcast to playlist because of data conflict: playlistId={}, podcastId={}",
                    playlistId,
                    request.podcastId()
            );
            throw new ConflictException("Podcast already exists in playlist");
        }

        log.info(
                "Podcast added to playlist: playlistId={}, podcastId={}, userId={}, position={}",
                playlistId,
                request.podcastId(),
                currentUserId,
                item.getPosition()
        );

        return detailAfterMutation(playlist, currentUser, currentUserId);
    }

    @Transactional
    public void removePodcast(UUID playlistId, UUID podcastId, UUID currentUserId) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        ensureOwner(playlist, currentUser.getId());

        PlaylistPodcastEntity item = playlistPodcastRepository
                .findByIdPlaylistIdAndIdPodcastId(playlistId, podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found in playlist"));

        int removedPosition = item.getPosition();
        playlistPodcastRepository.delete(item);
        playlistPodcastRepository.flush();
        playlistPodcastRepository.closeGapAfterDelete(playlistId, removedPosition);

        log.info(
                "Podcast removed from playlist: playlistId={}, podcastId={}, userId={}, oldPosition={}",
                playlistId,
                podcastId,
                currentUserId,
                removedPosition
        );
    }

    @Transactional
    public PlaylistDetailResponse reorder(UUID playlistId, UUID currentUserId, ReorderPlaylistRequest request) {
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));

        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        ensureOwner(playlist, currentUser.getId());

        List<PlaylistPodcastEntity> items = playlistPodcastRepository.findByPlaylistIdForUpdate(playlistId);
        validateReorderRequest(request, items);

        playlistPodcastRepository.offsetPositions(playlistId, items.size() + 1);
        playlistPodcastRepository.flush();

        request.items().stream()
                .sorted(Comparator.comparingInt(ReorderPlaylistRequest.Item::position))
                .forEach(reorderItem -> playlistPodcastRepository.setPosition(
                        playlistId,
                        reorderItem.podcastId(),
                        reorderItem.position()
                ));
        playlistPodcastRepository.flush();

        log.info("Playlist reordered: playlistId={}, userId={}, items={}", playlistId, currentUserId, items.size());

        return detailAfterMutation(playlist, currentUser, currentUserId);
    }

    @Transactional
    public VoteResponse vote(UUID playlistId, UUID currentUserId, VoteRequest request) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));
        ensureVisible(playlist, currentUser.getId());

        PlaylistVoteEntity vote = playlistVoteRepository
                .findByIdUserProfileIdAndIdPlaylistId(currentUser.getId(), playlistId)
                .orElse(null);

        if (vote == null) {
            vote = new PlaylistVoteEntity();
            vote.setId(new PlaylistVoteId(currentUser.getId(), playlistId));
            vote.setUserProfile(currentUser);
            vote.setPlaylist(playlist);
            vote.setVoteType(request.voteType());
            applyVoteDelta(playlist, null, request.voteType());
            playlistVoteRepository.save(vote);
        } else if (vote.getVoteType() != request.voteType()) {
            VoteType previousVote = vote.getVoteType();
            vote.setVoteType(request.voteType());
            applyVoteDelta(playlist, previousVote, request.voteType());
        }

        PlaylistEntity saved = playlistRepository.saveAndFlush(playlist);
        playlistVoteRepository.flush();

        log.info(
                "Playlist vote saved: playlistId={}, userId={}, voteType={}",
                playlistId,
                currentUserId,
                request.voteType()
        );

        return new VoteResponse(
                saved.getId(),
                "PLAYLIST",
                saved.getLikesCount(),
                saved.getDislikesCount(),
                request.voteType()
        );
    }

    @Transactional
    public VoteResponse removeVote(UUID playlistId, UUID currentUserId) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        PlaylistEntity playlist = playlistRepository.findWithOwnerByIdForUpdate(playlistId)
                .orElseThrow(() -> new NotFoundException("Playlist not found"));
        ensureVisible(playlist, currentUser.getId());

        PlaylistVoteEntity vote = playlistVoteRepository
                .findByIdUserProfileIdAndIdPlaylistId(currentUser.getId(), playlistId)
                .orElse(null);

        if (vote != null) {
            applyVoteDelta(playlist, vote.getVoteType(), null);
            playlistVoteRepository.delete(vote);
            playlistVoteRepository.flush();
            playlistRepository.saveAndFlush(playlist);
        }

        log.info("Playlist vote removed: playlistId={}, userId={}", playlistId, currentUserId);

        return new VoteResponse(
                playlist.getId(),
                "PLAYLIST",
                playlist.getLikesCount(),
                playlist.getDislikesCount(),
                null
        );
    }

    private PageResponse<PlaylistCard> mapPage(Page<PlaylistEntity> page, int requestedPage, UUID currentUserProfileId) {
        Page<PlaylistCard> mapped = page.map(entity -> playlistMapper.toCard(entity, currentUserProfileId));
        return new PageResponse<>(
                mapped.getContent(),
                new PageMeta(
                        Math.max(requestedPage, 1),
                        mapped.getSize(),
                        mapped.getTotalElements(),
                        mapped.getTotalPages()
                )
        );
    }

    private PlaylistDetailResponse detailAfterMutation(
            PlaylistEntity playlist,
            UserProfileEntity currentUser,
            UUID currentUserId
    ) {
        List<PlaylistPodcastEntity> items = playlistPodcastRepository.findByIdPlaylistIdOrderByPositionAsc(playlist.getId());
        return playlistMapper.toDetail(playlist, items, currentUser.getId(), resolveAuthorIdByUserId(currentUserId));
    }

    private void validateReorderRequest(ReorderPlaylistRequest request, List<PlaylistPodcastEntity> existingItems) {
        if (request.items().size() != existingItems.size()) {
            throw new BusinessRuleException("Reorder request must contain every podcast from playlist exactly once");
        }

        Set<UUID> existingPodcastIds = existingItems.stream()
                .map(item -> item.getPodcast().getId())
                .collect(Collectors.toSet());
        Set<UUID> requestedPodcastIds = new HashSet<>();
        Set<Integer> requestedPositions = new HashSet<>();

        for (ReorderPlaylistRequest.Item item : request.items()) {
            if (!requestedPodcastIds.add(item.podcastId())) {
                throw new BadRequestException("Duplicate podcastId in reorder request");
            }
            if (!requestedPositions.add(item.position())) {
                throw new BadRequestException("Duplicate position in reorder request");
            }
        }

        if (!Objects.equals(existingPodcastIds, requestedPodcastIds)) {
            throw new BusinessRuleException("Reorder request contains podcastId not from this playlist");
        }

        for (int position = 1; position <= existingItems.size(); position++) {
            if (!requestedPositions.contains(position)) {
                throw new BadRequestException("Positions must be contiguous and start with 1");
            }
        }
    }

    private void applyVoteDelta(PlaylistEntity playlist, VoteType previousVote, VoteType nextVote) {
        if (previousVote == VoteType.LIKE) {
            playlist.setLikesCount(Math.max(0, playlist.getLikesCount() - 1));
        } else if (previousVote == VoteType.DISLIKE) {
            playlist.setDislikesCount(Math.max(0, playlist.getDislikesCount() - 1));
        }

        if (nextVote == VoteType.LIKE) {
            playlist.setLikesCount(playlist.getLikesCount() + 1);
        } else if (nextVote == VoteType.DISLIKE) {
            playlist.setDislikesCount(playlist.getDislikesCount() + 1);
        }
    }

    private UserProfileEntity requireUserProfile(UUID userId) {
        if (userId == null) {
            throw new ForbiddenOperationException("Current user is not authenticated");
        }
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + userId));
    }

    private UUID resolveUserProfileId(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userProfileRepository.findByUserId(userId)
                .map(UserProfileEntity::getId)
                .orElse(null);
    }

    private UUID resolveAuthorIdByUserId(UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return authorRepository.findByUserProfileUserId(currentUserId)
                .map(AuthorEntity::getId)
                .orElse(null);
    }

    private void ensureVisible(PlaylistEntity playlist, UUID currentUserProfileId) {
        if (playlist.isPublicPlaylist()) {
            return;
        }

        if (currentUserProfileId != null && playlist.getOwner().getId().equals(currentUserProfileId)) {
            return;
        }

        throw new ForbiddenOperationException("You don't have permission to view this resource");
    }

    private void ensureOwner(PlaylistEntity playlist, UUID currentUserProfileId) {
        if (!playlist.getOwner().getId().equals(currentUserProfileId)) {
            throw new ForbiddenOperationException("You don't have permission to modify this resource");
        }
    }

    private void validateUpdateRequest(UpdatePlaylistRequest request) {
        if (!request.isTitleSet()
                && !request.isDescriptionSet()
                && !request.isCoverImageUrlSet()
                && !request.isPublicSet()) {
            throw new BadRequestException("At least one field must be provided for update");
        }
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }

        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUri(String value, String fieldName) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            return null;
        }

        try {
            URI uri = new URI(normalized);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new BadRequestException(fieldName + " must be an absolute URI");
            }
            return normalized;
        } catch (URISyntaxException exception) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put(fieldName, "must be a valid URI");
            throw new BadRequestException("Request validation failed", Map.of("fields", fields));
        }
    }
}
