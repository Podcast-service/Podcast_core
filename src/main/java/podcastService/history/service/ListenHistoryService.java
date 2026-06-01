package podcastService.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.history.dto.ListenHistoryItem;
import podcastService.history.dto.SaveProgressRequest;
import podcastService.history.entity.ListenHistoryEntity;
import podcastService.history.repository.ListenHistoryRepository;
import podcastService.infrastructure.outbox.recommendation.PodcastActivityEventFactory;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteType;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListenHistoryService {

    private static final double COMPLETION_THRESHOLD = 0.95d;

    private final ListenHistoryRepository listenHistoryRepository;
    private final UserProfileRepository userProfileRepository;
    private final PodcastRepository podcastRepository;
    private final PodcastVoteRepository podcastVoteRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthorRepository authorRepository;
    private final PodcastMapper podcastMapper;
    private final RecommendationOutboxEventService recommendationOutboxEventService;

    @Transactional(readOnly = true)
    public PageResponse<ListenHistoryItem> listMine(UUID currentUserId, int page, int size) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);

        Page<ListenHistoryEntity> historyPage = listenHistoryRepository
                .findByIdUserProfileIdOrderByLastListenedAtDesc(
                        currentUser.getId(),
                        PageRequest.of(normalizedPage - 1, normalizedSize)
                );

        Set<UUID> authorIds = historyPage.getContent().stream()
                .map(item -> item.getPodcast().getAuthor().getId())
                .collect(Collectors.toSet());
        Set<UUID> subscribedAuthorIds = authorIds.isEmpty()
                ? Set.of()
                : subscriptionRepository.findSubscribedAuthorIds(currentUser.getId(), authorIds);
        Map<UUID, VoteType> currentUserVotes = resolveCurrentUserVotes(historyPage, currentUser.getId());
        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);

        Page<ListenHistoryItem> mapped = historyPage.map(item -> toHistoryItem(
                item,
                currentAuthorId,
                subscribedAuthorIds,
                currentUserVotes
        ));

        log.debug(
                "Listen history listed: currentUserId={}, page={}, size={}, totalElements={}",
                currentUserId,
                normalizedPage,
                normalizedSize,
                mapped.getTotalElements()
        );

        return new PageResponse<>(
                mapped.getContent(),
                new PageMeta(normalizedPage, mapped.getSize(), mapped.getTotalElements(), mapped.getTotalPages())
        );
    }

    @Transactional
    public void saveProgress(UUID podcastId, UUID currentUserId, SaveProgressRequest request) {
        if (request.progressSeconds() == null) {
            throw new BadRequestException("progressSeconds must not be null");
        }

        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        PodcastEntity podcast = podcastRepository.findDetailedById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        if (podcast.getStatus() != Status.PUBLISHED) {
            throw new NotFoundException("Podcast not found");
        }

        int progressSeconds = normalizeProgress(request.progressSeconds(), podcast.getDurationSeconds());
        boolean completed = isCompleted(progressSeconds, podcast.getDurationSeconds());
        Optional<ListenHistoryEntity> previousHistory = listenHistoryRepository
                .findByIdUserProfileIdAndIdPodcastId(currentUser.getId(), podcastId);
        boolean newlyCompleted = completed && previousHistory.map(item -> !item.isCompleted()).orElse(true);

        listenHistoryRepository.upsertProgress(currentUser.getId(), podcastId, progressSeconds, completed);
        if (newlyCompleted) {
            recommendationOutboxEventService.saveUserActivityEvent(
                    currentUserId,
                    PodcastActivityEventFactory.playFinished(
                            podcastId,
                            currentUserId,
                            progressSeconds,
                            Instant.now(),
                            null,
                            null
                    )
            );
        }

        log.info(
                "Listen progress saved: podcastId={}, currentUserId={}, progressSeconds={}, completed={}",
                podcastId,
                currentUserId,
                progressSeconds,
                completed
        );
    }

    private ListenHistoryItem toHistoryItem(
            ListenHistoryEntity item,
            UUID currentAuthorId,
            Set<UUID> subscribedAuthorIds,
            Map<UUID, VoteType> currentUserVotes
    ) {
        PodcastEntity podcast = item.getPodcast();
        PodcastCard card = podcastMapper.toCard(podcast, currentAuthorId, subscribedAuthorIds, currentUserVotes);
        return new ListenHistoryItem(
                card,
                item.getProgressSeconds(),
                progressPercent(item.getProgressSeconds(), podcast.getDurationSeconds()),
                item.isCompleted(),
                item.getLastListenedAt()
        );
    }

    private int normalizeProgress(int progressSeconds, Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return progressSeconds;
        }
        return (int) Math.min(progressSeconds, durationSeconds);
    }

    private boolean isCompleted(int progressSeconds, Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return false;
        }
        return progressSeconds >= Math.ceil(durationSeconds * COMPLETION_THRESHOLD);
    }

    private Integer progressPercent(int progressSeconds, Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return null;
        }
        return (int) Math.min(100, Math.round(progressSeconds * 100.0d / durationSeconds));
    }

    private Map<UUID, VoteType> resolveCurrentUserVotes(Page<ListenHistoryEntity> historyPage, UUID currentUserProfileId) {
        Set<UUID> podcastIds = historyPage.getContent().stream()
                .map(item -> item.getPodcast().getId())
                .collect(Collectors.toSet());

        if (podcastIds.isEmpty()) {
            return Map.of();
        }

        return podcastVoteRepository.findByUserProfileIdAndPodcastIds(currentUserProfileId, podcastIds)
                .stream()
                .collect(Collectors.toMap(
                        vote -> vote.getId().getPodcastId(),
                        PodcastVoteEntity::getVoteType
                ));
    }

    private UUID resolveAuthorIdByUserId(UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }

        return authorRepository.findByUserProfileUserId(currentUserId)
                .map(AuthorEntity::getId)
                .orElse(null);
    }

    private UserProfileEntity requireUserProfile(UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenOperationException("Current user is not authenticated");
        }
        return userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
    }
}
