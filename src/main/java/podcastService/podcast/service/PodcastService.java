package podcastService.podcast.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.category.entity.CategoryEntity;
import podcastService.category.repository.CategoryRepository;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.BusinessRuleException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.podcast.dto.CreatePodcastRequest;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.dto.PodcastFilter;
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
        PodcastEntity podcast = podcastRepository.findDetailedById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);

        boolean visible = podcast.getStatus() == Status.PUBLISHED
                || (currentAuthorId != null && currentAuthorId.equals(podcast.getAuthor().getId()));

        if (!visible) {
            log.warn(
                    "Access denied to podcast details: podcastId={}, status={}, currentUserId={}, currentAuthorId={}, ownerAuthorId={}",
                    podcastId,
                    podcast.getStatus(),
                    currentUserId,
                    currentAuthorId,
                    podcast.getAuthor().getId()
            );
            throw new NotFoundException("Podcast not found");
        }

        return podcastMapper.toDetail(
                podcast,
                currentAuthorId,
                resolveSubscribedAuthorIds(Set.of(podcast.getAuthor().getId()), currentUserProfileId),
                resolveCurrentUserVote(podcast.getId(), currentUserProfileId),
                hasTranscript(podcast.getId()),
                hasSummary(podcast.getId())
        );
    }

    @Transactional
    public PodcastDetailResponse update(UUID podcastId, UUID currentUserId, UpdatePodcastRequest request) {
        validateUpdateRequest(request);

        PodcastEntity podcast = podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        UUID currentAuthorId = requireCurrentAuthorId(currentUserId);
        ensureOwner(podcast, currentAuthorId);

        if (podcast.getStatus() == Status.PROCESSING) {
            throw new BusinessRuleException("Cannot update a podcast in PROCESSING status");
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

        if (podcast.getStatus() == Status.PROCESSING) {
            throw new BusinessRuleException("Cannot publish a podcast in PROCESSING status");
        }

        if (podcast.getStatus() == Status.PUBLISHED) {
            throw new BusinessRuleException("Cannot publish a podcast in PUBLISHED status");
        }

        if (podcast.getStatus() == Status.ARCHIVED) {
            throw new BusinessRuleException("Cannot publish an archived podcast");
        }

        if (isBlank(podcast.getAudioUrl())) {
            throw new BusinessRuleException("Cannot publish a podcast without uploaded audio");
        }

        podcast.setStatus(Status.PROCESSING);

        PodcastEntity saved = podcastRepository.saveAndFlush(podcast);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);

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
        return podcastTranscriptRepository.existsByIdPodcastId(podcastId);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
