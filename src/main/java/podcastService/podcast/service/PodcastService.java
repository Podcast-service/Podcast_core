package podcastService.podcast.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.category.entity.CategoryEntity;
import podcastService.category.repository.CategoryRepository;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.AuthorProfileNotFoundException;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.BusinessRuleException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.outbox.recommendation.PodcastContentEventFactory;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.dto.CreatePodcastRequest;
import podcastService.podcast.dto.LikedPodcastsSort;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.dto.PodcastFilter;
import podcastService.podcast.dto.PodcastSpeakersResponse;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.dto.UpdatePodcastRequest;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.podcast.specifications.PodcastSpecifications;
import podcastService.podcast.util.PodcastPageableFactory;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastService {

    private final PodcastRepository podcastRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PodcastMapper podcastMapper;
    private final UserProfileRepository userProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PodcastVoteRepository podcastVoteRepository;
    private final PodcastTranscriptRepository podcastTranscriptRepository;
    private final PodcastSummaryRepository podcastSummaryRepository;
    private final PodcastMediaStatusTransitionPolicy mediaStatusTransitionPolicy;
    private final RecommendationOutboxEventService recommendationOutboxEventService;

    @Transactional(readOnly = true)
    public PageResponse<PodcastCard> list(PodcastFilter filter, UUID currentUserId) {
        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);

        Specification<PodcastEntity> specification = Specification
                .where(PodcastSpecifications.fetchRelations())
                .and(PodcastSpecifications.withPublishedOnly())
                .and(PodcastSpecifications.searchByText(filter.q()))
                .and(PodcastSpecifications.withCategoryId(filter.categoryId()))
                .and(PodcastSpecifications.withAuthorId(filter.authorId()));

        Page<PodcastEntity> podcastPage = podcastRepository.findAll(
                specification,
                PodcastPageableFactory.create(
                        filter.normalizedPage(),
                        filter.normalizedSize(),
                        filter.normalizedSort()
                )
        );
        Set<UUID> subscribedAuthorIds = resolveSubscribedAuthorIds(podcastPage, currentUserProfileId);
        Map<UUID, VoteType> currentUserVotes = resolveCurrentUserVotes(podcastPage, currentUserProfileId);
        Page<PodcastCard> page = podcastPage.map(entity -> podcastMapper.toCard(
                entity,
                currentAuthorId,
                subscribedAuthorIds,
                currentUserVotes
        ));

        log.debug(
                "Podcasts listed: currentUserId={}, currentAuthorId={}, page={}, size={}, totalElements={}",
                currentUserId,
                currentAuthorId,
                filter.normalizedPage(),
                filter.normalizedSize(),
                page.getTotalElements()
        );

        return new PageResponse<>(
                page.getContent(),
                new PageMeta(
                        filter.normalizedPage(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PodcastCard> listByAuthor(
            UUID authorId,
            String query,
            SortPodcasts sort,
            int page,
            int size,
            UUID currentUserId
    ) {
        authorRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        PodcastFilter filter = new PodcastFilter(query, null, authorId, sort, page, size);

        log.debug(
                "Author podcasts requested: authorId={}, currentUserId={}, page={}, size={}",
                authorId,
                currentUserId,
                filter.normalizedPage(),
                filter.normalizedSize()
        );

        return list(filter, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PodcastDetailResponse> listMineAsAuthor(
            UUID currentUserId,
            Status status,
            String query,
            SortPodcasts sort,
            int page,
            int size
    ) {
        AuthorEntity author = authorRepository.findByUserProfileUserId(currentUserId)
                .orElseThrow(() -> new AuthorProfileNotFoundException("Author profile not found"));

        Specification<PodcastEntity> specification = Specification
                .where(PodcastSpecifications.fetchRelations())
                .and(PodcastSpecifications.withAuthorId(author.getId()))
                .and(PodcastSpecifications.withStatus(status))
                .and(PodcastSpecifications.searchByText(query));

        int normalizedPage = Math.max(page, 1);
        Page<PodcastEntity> podcastPage = podcastRepository.findAll(
                specification,
                ownerPodcastPageable(page, size, sort)
        );

        log.debug(
                "Current author podcasts listed: currentUserId={}, authorId={}, status={}, page={}, size={}, totalElements={}",
                currentUserId,
                author.getId(),
                status,
                normalizedPage,
                podcastPage.getSize(),
                podcastPage.getTotalElements()
        );

        return mapDetailPage(
                podcastPage,
                normalizedPage,
                author.getId(),
                author.getUserProfile().getId()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PodcastCard> listLikedByCurrentUser(
            UUID currentUserId,
            LikedPodcastsSort sort,
            int page,
            int size
    ) {
        UserProfileEntity currentUserProfile = userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + currentUserId));

        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(normalizedPage - 1, normalizedSize);
        LikedPodcastsSort normalizedSort = sort == null ? LikedPodcastsSort.DATE_DESC : sort;

        Page<PodcastEntity> podcastPage = normalizedSort == LikedPodcastsSort.DATE_ASC
                ? podcastVoteRepository.findVotedPodcastsByVoteDateAsc(
                        currentUserProfile.getId(),
                        VoteType.LIKE,
                        Status.PUBLISHED,
                        pageable
                )
                : podcastVoteRepository.findVotedPodcastsByVoteDateDesc(
                        currentUserProfile.getId(),
                        VoteType.LIKE,
                        Status.PUBLISHED,
                        pageable
                );

        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);
        Set<UUID> subscribedAuthorIds = resolveSubscribedAuthorIds(podcastPage, currentUserProfile.getId());
        Map<UUID, VoteType> currentUserVotes = resolveCurrentUserVotes(podcastPage, currentUserProfile.getId());
        Page<PodcastCard> mapped = podcastPage.map(entity -> podcastMapper.toCard(
                entity,
                currentAuthorId,
                subscribedAuthorIds,
                currentUserVotes
        ));

        log.debug(
                "Liked podcasts listed: currentUserId={}, page={}, size={}, totalElements={}",
                currentUserId,
                normalizedPage,
                mapped.getSize(),
                mapped.getTotalElements()
        );

        return new PageResponse<>(
                mapped.getContent(),
                new PageMeta(
                        normalizedPage,
                        mapped.getSize(),
                        mapped.getTotalElements(),
                        mapped.getTotalPages()
                )
        );
    }

    @Transactional
    public PodcastDetailResponse create(UUID currentUserId, CreatePodcastRequest request) {
        AuthorEntity author = authorRepository.findByUserProfileUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenOperationException("Current user does not have author profile"));

        CategoryEntity category = resolveCategory(request.categoryId());

        PodcastEntity entity = new PodcastEntity();
        entity.setAuthor(author);
        entity.setCategory(category);
        entity.setTitle(normalizeRequiredText(request.title(), "title"));
        entity.setDescription(normalizeNullableText(request.description()));
        entity.setCoverImageUrl(normalizeNullableText(request.coverImageUrl()));
        entity.setNumSpeakers(validateNumSpeakers(request.numSpeakers()));
        entity.setStatus(Status.DRAFT);

        PodcastEntity saved = podcastRepository.saveAndFlush(entity);

        log.info(
                "Podcast created: podcastId={}, authorId={}, userId={}, status={}",
                saved.getId(),
                author.getId(),
                currentUserId,
                saved.getStatus()
        );

        return podcastMapper.toDetail(
                saved,
                author.getId(),
                resolveSubscribedAuthorIds(Set.of(author.getId()), author.getUserProfile().getId()),
                null,
                false,
                false
        );
    }

    @Transactional(readOnly = true)
    public PodcastDetailResponse getById(UUID podcastId, UUID currentUserId) {
        VisiblePodcast visiblePodcast = findVisiblePodcast(podcastId, currentUserId, "details");
        PodcastEntity podcast = visiblePodcast.podcast();

        return podcastMapper.toDetail(
                podcast,
                visiblePodcast.currentAuthorId(),
                resolveSubscribedAuthorIds(Set.of(podcast.getAuthor().getId()), visiblePodcast.currentUserProfileId()),
                resolveCurrentUserVote(podcast.getId(), visiblePodcast.currentUserProfileId()),
                hasTranscript(podcast.getId()),
                hasSummary(podcast.getId())
        );
    }

    @Transactional(readOnly = true)
    public PodcastSpeakersResponse getSpeakersById(UUID podcastId, UUID currentUserId) {
        PodcastEntity podcast = podcastRepository.findDetailedById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));
        return new PodcastSpeakersResponse(podcast.getId(), podcast.getNumSpeakers());
    }

    @Transactional
    public PodcastDetailResponse update(UUID podcastId, UUID currentUserId, UpdatePodcastRequest request) {
        validateUpdateRequest(request);

        PodcastEntity podcast = podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        UUID currentAuthorId = requireCurrentAuthorId(currentUserId);
        ensureOwner(podcast, currentAuthorId);

        if (podcast.getStatus() == Status.UPLOADING || podcast.getStatus() == Status.PROCESSING) {
            throw new BusinessRuleException("Cannot update a podcast while media pipeline is active");
        }

        if (podcast.getStatus() == Status.ARCHIVED) {
            throw new BusinessRuleException("Cannot update an archived podcast");
        }

        if (request.isTitleSet()) {
            podcast.setTitle(normalizeRequiredText(request.getTitle(), "title"));
        }

        if (request.isDescriptionSet()) {
            podcast.setDescription(normalizeNullableText(request.getDescription()));
        }

        if (request.isCategoryIdSet()) {
            podcast.setCategory(resolveCategory(request.getCategoryId()));
        }

        if (request.isCoverImageUrlSet()) {
            podcast.setCoverImageUrl(normalizeNullableText(request.getCoverImageUrl()));
        }

        PodcastEntity saved = podcastRepository.saveAndFlush(podcast);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);
        if (saved.getStatus() == Status.PUBLISHED) {
            savePublishedPodcastUpdateEvent(saved, currentUserId);
        }

        log.info(
                "Podcast updated: podcastId={}, currentUserId={}, status={}, titleChanged={}, descriptionChanged={}, categoryChanged={}, coverChanged={}",
                saved.getId(),
                currentUserId,
                saved.getStatus(),
                request.isTitleSet(),
                request.isDescriptionSet(),
                request.isCategoryIdSet(),
                request.isCoverImageUrlSet()
        );

        return podcastMapper.toDetail(
                saved,
                currentAuthorId,
                resolveSubscribedAuthorIds(Set.of(saved.getAuthor().getId()), currentUserProfileId),
                resolveCurrentUserVote(saved.getId(), currentUserProfileId),
                hasTranscript(saved.getId()),
                hasSummary(saved.getId())
        );
    }

    @Transactional
    public void archive(UUID podcastId, UUID currentUserId) {
        PodcastEntity podcast = podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        UUID currentAuthorId = requireCurrentAuthorId(currentUserId);
        ensureOwner(podcast, currentAuthorId);

        if (podcast.getStatus() == Status.ARCHIVED) {
            log.info(
                    "Podcast already archived: podcastId={}, currentUserId={}",
                    podcastId,
                    currentUserId
            );
            return;
        }

        podcast.setStatus(Status.ARCHIVED);
        podcastRepository.saveAndFlush(podcast);
        recommendationOutboxEventService.savePodcastContentEvent(
                podcast.getId(),
                PodcastContentEventFactory.deleted(
                        podcast.getId(),
                        podcast.getAuthor().getId(),
                        podcast.getCategory() == null ? null : podcast.getCategory().getId(),
                        Instant.now(),
                        currentUserId,
                        null,
                        null
                )
        );

        log.info(
                "Podcast archived: podcastId={}, currentUserId={}, ownerAuthorId={}",
                podcastId,
                currentUserId,
                podcast.getAuthor().getId()
        );
    }

    @Transactional
    public PodcastDetailResponse publish(UUID podcastId, UUID currentUserId) {
        PodcastEntity podcast = podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        UUID currentAuthorId = requireCurrentAuthorId(currentUserId);
        ensureOwner(podcast, currentAuthorId);

        if (podcast.getStatus() == Status.PUBLISHED) {
            throw new BusinessRuleException("Cannot publish a podcast in PUBLISHED status");
        }

        if (podcast.getStatus() == Status.ARCHIVED) {
            throw new BusinessRuleException("Cannot publish an archived podcast");
        }

        if (podcast.getStatus() == Status.FAILED) {
            throw new BusinessRuleException("Cannot publish a podcast with media processing error");
        }

        if (!mediaStatusTransitionPolicy.allowsPublication(podcast.getStatus())) {
            throw new BusinessRuleException("Cannot publish a podcast before media is processed");
        }

        if (isBlank(podcast.getAudioUrl())) {
            throw new BusinessRuleException("Cannot publish a podcast without processed audio_url");
        }

        if (podcast.getDurationSeconds() == null || podcast.getDurationSeconds() <= 0) {
            throw new BusinessRuleException("Cannot publish a podcast without positive duration_seconds");
        }

        podcast.setStatus(Status.PUBLISHED);

        PodcastEntity saved = podcastRepository.saveAndFlush(podcast);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);
        savePodcastPublishedEvent(saved, currentUserId);

        log.info(
                "Podcast sent to processing: podcastId={}, currentUserId={}, ownerAuthorId={}, status={}",
                saved.getId(),
                currentUserId,
                currentAuthorId,
                saved.getStatus()
        );

        return podcastMapper.toDetail(
                saved,
                currentAuthorId,
                resolveSubscribedAuthorIds(Set.of(saved.getAuthor().getId()), currentUserProfileId),
                resolveCurrentUserVote(saved.getId(), currentUserProfileId),
                hasTranscript(saved.getId()),
                hasSummary(saved.getId())
        );
    }

    private Set<UUID> resolveSubscribedAuthorIds(Page<PodcastEntity> podcastPage, UUID currentUserProfileId) {
        if (currentUserProfileId == null) {
            return null;
        }

        Set<UUID> authorIds = podcastPage.getContent().stream()
                .map(podcast -> podcast.getAuthor().getId())
                .collect(Collectors.toSet());

        return resolveSubscribedAuthorIds(authorIds, currentUserProfileId);
    }

    private PageResponse<PodcastDetailResponse> mapDetailPage(
            Page<PodcastEntity> podcastPage,
            int requestedPage,
            UUID currentAuthorId,
            UUID currentUserProfileId
    ) {
        Set<UUID> subscribedAuthorIds = resolveSubscribedAuthorIds(podcastPage, currentUserProfileId);
        Map<UUID, VoteType> currentUserVotes = resolveCurrentUserVotes(podcastPage, currentUserProfileId);
        Set<UUID> podcastIds = podcastPage.getContent().stream()
                .map(PodcastEntity::getId)
                .collect(Collectors.toSet());
        Set<UUID> podcastIdsWithTranscripts = podcastIds.isEmpty()
                ? Set.of()
                : podcastTranscriptRepository.findPodcastIdsWithContent(podcastIds);
        Set<UUID> podcastIdsWithSummaries = podcastIds.isEmpty()
                ? Set.of()
                : podcastSummaryRepository.findPodcastIdsWithSummary(podcastIds);

        Page<PodcastDetailResponse> mapped = podcastPage.map(entity -> podcastMapper.toDetail(
                entity,
                currentAuthorId,
                subscribedAuthorIds,
                currentUserVotes == null ? null : currentUserVotes.get(entity.getId()),
                podcastIdsWithTranscripts.contains(entity.getId()),
                podcastIdsWithSummaries.contains(entity.getId())
        ));

        return new PageResponse<>(
                mapped.getContent(),
                new PageMeta(
                        requestedPage,
                        mapped.getSize(),
                        mapped.getTotalElements(),
                        mapped.getTotalPages()
                )
        );
    }

    private Pageable ownerPodcastPageable(int page, int size, SortPodcasts sort) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        SortPodcasts normalizedSort = sort == null ? SortPodcasts.DATE_DESC : sort;

        Sort mappedSort = switch (normalizedSort) {
            case DATE_ASC -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id")
            );
            case RATING -> Sort.by(
                    Sort.Order.desc("likesCount"),
                    Sort.Order.asc("dislikesCount"),
                    Sort.Order.desc("viewsCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case VIEWS -> Sort.by(
                    Sort.Order.desc("viewsCount"),
                    Sort.Order.desc("likesCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
            case DATE_DESC -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };

        return PageRequest.of(normalizedPage - 1, normalizedSize, mappedSort);
    }

    private Set<UUID> resolveSubscribedAuthorIds(Set<UUID> authorIds, UUID currentUserProfileId) {
        if (currentUserProfileId == null) {
            return null;
        }

        if (authorIds.isEmpty()) {
            return Set.of();
        }

        return subscriptionRepository.findSubscribedAuthorIds(currentUserProfileId, authorIds);
    }

    private UUID resolveUserProfileId(UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }

        return userProfileRepository.findByUserId(currentUserId)
                .map(UserProfileEntity::getId)
                .orElse(null);
    }

    private Map<UUID, VoteType> resolveCurrentUserVotes(Page<PodcastEntity> podcastPage, UUID currentUserProfileId) {
        if (currentUserProfileId == null) {
            return null;
        }

        Set<UUID> podcastIds = podcastPage.getContent().stream()
                .map(PodcastEntity::getId)
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

    private VoteType resolveCurrentUserVote(UUID podcastId, UUID currentUserProfileId) {
        if (currentUserProfileId == null) {
            return null;
        }

        return podcastVoteRepository.findByIdUserProfileIdAndIdPodcastId(currentUserProfileId, podcastId)
                .map(PodcastVoteEntity::getVoteType)
                .orElse(null);
    }

    private boolean hasTranscript(UUID podcastId) {
        return podcastTranscriptRepository.existsByIdPodcastIdAndContentIsNotNull(podcastId);
    }

    private boolean hasSummary(UUID podcastId) {
        return podcastSummaryRepository.existsByIdPodcastId(podcastId);
    }

    private UUID requireCurrentAuthorId(UUID currentUserId) {
        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);
        if (currentAuthorId == null) {
            throw new ForbiddenOperationException("Current user does not have author profile");
        }
        return currentAuthorId;
    }

    private UUID resolveAuthorIdByUserId(UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }

        return authorRepository.findByUserProfileUserId(currentUserId)
                .map(AuthorEntity::getId)
                .orElse(null);
    }

    private void ensureOwner(PodcastEntity podcast, UUID currentAuthorId) {
        if (!podcast.getAuthor().getId().equals(currentAuthorId)) {
            throw new ForbiddenOperationException("You don't have permission to modify this resource");
        }
    }

    private CategoryEntity resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void validateUpdateRequest(UpdatePodcastRequest request) {
        if (!request.isTitleSet()
                && !request.isDescriptionSet()
                && !request.isCategoryIdSet()
                && !request.isCoverImageUrlSet()) {
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

    private int validateNumSpeakers(Integer value) {
        if (value == null) {
            throw new BadRequestException("num_speakers must not be null");
        }
        if (value < 1 || value > 32) {
            throw new BadRequestException("num_speakers must be between 1 and 32");
        }
        return value;
    }

    private VisiblePodcast findVisiblePodcast(UUID podcastId, UUID currentUserId, String resource) {
        PodcastEntity podcast = podcastRepository.findDetailedById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);

        boolean visible = podcast.getStatus() == Status.PUBLISHED
                || (currentAuthorId != null && currentAuthorId.equals(podcast.getAuthor().getId()));

        if (!visible) {
            log.warn(
                    "Access denied to podcast {}: podcastId={}, status={}, currentUserId={}, currentAuthorId={}, ownerAuthorId={}",
                    resource,
                    podcastId,
                    podcast.getStatus(),
                    currentUserId,
                    currentAuthorId,
                    podcast.getAuthor().getId()
            );
            throw new NotFoundException("Podcast not found");
        }

        return new VisiblePodcast(podcast, currentAuthorId, currentUserProfileId);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void savePodcastPublishedEvent(PodcastEntity podcast, UUID currentUserId) {
        if (!canWriteRecommendationCatalogEvent(podcast, RecommendationEventTypes.PODCAST_PUBLISHED)) {
            return;
        }

        Instant eventTime = Instant.now();
        recommendationOutboxEventService.savePodcastContentEvent(
                podcast.getId(),
                PodcastContentEventFactory.published(
                        podcast.getId(),
                        podcast.getAuthor().getId(),
                        podcast.getCategory().getId(),
                        podcast.getTitle(),
                        podcast.getDescription(),
                        podcast.getDurationSeconds(),
                        toInstant(podcast.getPublishedAt(), eventTime),
                        null,
                        null,
                        podcast.getStatus().name(),
                        null,
                        eventTime,
                        currentUserId,
                        null,
                        null
                )
        );
    }

    private void savePublishedPodcastUpdateEvent(PodcastEntity podcast, UUID currentUserId) {
        if (!recommendationOutboxEventService.enabled()) {
            return;
        }

        if (podcast.getCategory() == null) {
            log.warn(
                    "Recommendation podcast update emitted as tombstone: podcastId={}, reason=missingCategoryId",
                    podcast.getId()
            );
            recommendationOutboxEventService.savePodcastContentEvent(
                    podcast.getId(),
                    PodcastContentEventFactory.deleted(
                            podcast.getId(),
                            podcast.getAuthor().getId(),
                            null,
                            Instant.now(),
                            currentUserId,
                            null,
                            null
                    )
            );
            return;
        }

        Instant eventTime = Instant.now();
        recommendationOutboxEventService.savePodcastContentEvent(
                podcast.getId(),
                PodcastContentEventFactory.updated(
                        podcast.getId(),
                        podcast.getAuthor().getId(),
                        podcast.getCategory().getId(),
                        podcast.getTitle(),
                        podcast.getDescription(),
                        podcast.getDurationSeconds(),
                        toInstant(podcast.getPublishedAt(), eventTime),
                        null,
                        null,
                        podcast.getStatus().name(),
                        null,
                        eventTime,
                        eventTime,
                        currentUserId,
                        null,
                        null
                )
        );
    }

    private boolean canWriteRecommendationCatalogEvent(PodcastEntity podcast, String eventType) {
        if (!recommendationOutboxEventService.enabled()) {
            return false;
        }
        if (podcast.getCategory() != null) {
            return true;
        }

        log.warn(
                "Recommendation podcast content event skipped: podcastId={}, eventType={}, reason=missingCategoryId",
                podcast.getId(),
                eventType
        );
        return false;
    }

    private Instant toInstant(OffsetDateTime value, Instant fallback) {
        return value == null ? fallback : value.toInstant();
    }

    private record VisiblePodcast(
            PodcastEntity podcast,
            UUID currentAuthorId,
            UUID currentUserProfileId
    ) {
    }
}
