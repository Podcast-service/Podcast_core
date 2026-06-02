package podcastService.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.dto.AuthorCard;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.BusinessRuleException;
import podcastService.common.exception.ConflictException;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.outbox.recommendation.AuthorActivityEventFactory;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.podcast.util.PodcastPageableFactory;
import podcastService.subscription.dto.AuthorSubscriptionResponse;
import podcastService.subscription.dto.SubscriptionResponse;
import podcastService.subscription.entity.SubscriptionEntity;
import podcastService.subscription.entity.SubscriptionId;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteType;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorRepository authorRepository;
    private final PodcastRepository podcastRepository;
    private final PodcastVoteRepository podcastVoteRepository;
    private final PodcastMapper podcastMapper;
    private final RecommendationOutboxEventService recommendationOutboxEventService;

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> listMine(UUID currentUserId, int page, int size) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);

        Page<SubscriptionResponse> mapped = subscriptionRepository
                .findByIdSubscriberProfileIdOrderBySubscribedAtDesc(
                        currentUser.getId(),
                        org.springframework.data.domain.PageRequest.of(normalizedPage - 1, normalizedSize)
                )
                .map(subscription -> new SubscriptionResponse(
                        toAuthorCard(subscription.getAuthor(), true),
                        subscription.getSubscribedAt()
                ));

        log.debug(
                "Subscriptions listed: currentUserId={}, page={}, size={}, totalElements={}",
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

    @Transactional(readOnly = true)
    public PageResponse<PodcastCard> feed(UUID currentUserId, int page, int size, SortPodcasts sort) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        int normalizedPage = Math.max(page, 1);

        Page<PodcastEntity> podcastPage = podcastRepository.findSubscriptionFeed(
                currentUser.getId(),
                Status.PUBLISHED,
                PodcastPageableFactory.create(page, size, sort == null ? SortPodcasts.DATE_DESC : sort)
        );

        Set<UUID> authorIds = podcastPage.getContent().stream()
                .map(podcast -> podcast.getAuthor().getId())
                .collect(Collectors.toSet());
        Set<UUID> subscribedAuthorIds = authorIds.isEmpty()
                ? Set.of()
                : subscriptionRepository.findSubscribedAuthorIds(currentUser.getId(), authorIds);
        Map<UUID, VoteType> currentUserVotes = resolveCurrentUserVotes(podcastPage, currentUser.getId());
        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);

        Page<PodcastCard> mapped = podcastPage.map(podcast -> podcastMapper.toCard(
                podcast,
                currentAuthorId,
                subscribedAuthorIds,
                currentUserVotes
        ));

        log.debug(
                "Subscription feed listed: currentUserId={}, page={}, size={}, totalElements={}",
                currentUserId,
                normalizedPage,
                mapped.getSize(),
                mapped.getTotalElements()
        );

        return new PageResponse<>(
                mapped.getContent(),
                new PageMeta(normalizedPage, mapped.getSize(), mapped.getTotalElements(), mapped.getTotalPages())
        );
    }

    @Transactional
    public AuthorSubscriptionResponse subscribe(UUID authorId, UUID currentUserId) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        AuthorEntity author = authorRepository.findDetailedByIdForUpdate(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        if (author.getUserProfile().getId().equals(currentUser.getId())) {
            throw new BusinessRuleException("You cannot subscribe to your own author profile");
        }

        boolean existed = subscriptionRepository
                .existsByIdSubscriberProfileIdAndIdAuthorId(currentUser.getId(), authorId);

        if (!existed) {
            SubscriptionEntity subscription = new SubscriptionEntity();
            subscription.setId(new SubscriptionId(currentUser.getId(), authorId));
            subscription.setSubscriber(currentUser);
            subscription.setAuthor(author);

            try {
                subscriptionRepository.saveAndFlush(subscription);
            } catch (DataIntegrityViolationException exception) {
                log.warn("Subscription conflict while subscribing: authorId={}, currentUserId={}", authorId, currentUserId);
                throw new ConflictException("Subscription already exists or violates constraints");
            }

            author.setSubscribersCount(author.getSubscribersCount() + 1);
            authorRepository.saveAndFlush(author);
            recommendationOutboxEventService.saveAuthorEvent(
                    authorId,
                    AuthorActivityEventFactory.followed(
                            authorId,
                            currentUserId,
                            Instant.now(),
                            null,
                            null
                    )
            );
        }

        log.info(
                "Author subscribed: authorId={}, currentUserId={}, alreadyExisted={}",
                authorId,
                currentUserId,
                existed
        );

        return new AuthorSubscriptionResponse(author.getId(), author.getSubscribersCount(), true);
    }

    @Transactional
    public AuthorSubscriptionResponse unsubscribe(UUID authorId, UUID currentUserId) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        AuthorEntity author = authorRepository.findDetailedByIdForUpdate(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        SubscriptionEntity subscription = subscriptionRepository
                .findById(new SubscriptionId(currentUser.getId(), authorId))
                .orElse(null);

        if (subscription != null) {
            subscriptionRepository.delete(subscription);
            subscriptionRepository.flush();
            author.setSubscribersCount(Math.max(0, author.getSubscribersCount() - 1));
            authorRepository.saveAndFlush(author);
            recommendationOutboxEventService.saveAuthorEvent(
                    authorId,
                    AuthorActivityEventFactory.unfollowed(
                            authorId,
                            currentUserId,
                            Instant.now(),
                            null,
                            null
                    )
            );
        }

        log.info(
                "Author unsubscribed: authorId={}, currentUserId={}, existed={}",
                authorId,
                currentUserId,
                subscription != null
        );

        return new AuthorSubscriptionResponse(author.getId(), author.getSubscribersCount(), false);
    }

    private Map<UUID, VoteType> resolveCurrentUserVotes(Page<PodcastEntity> podcastPage, UUID currentUserProfileId) {
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

    private UUID resolveAuthorIdByUserId(UUID currentUserId) {
        return authorRepository.findByUserProfileUserId(currentUserId)
                .map(AuthorEntity::getId)
                .orElse(null);
    }

    private AuthorCard toAuthorCard(AuthorEntity author, Boolean isSubscribed) {
        return new AuthorCard(
                author.getId(),
                author.getAuthorName(),
                author.getUserProfile() == null ? null : author.getUserProfile().getAvatarUrl(),
                author.getSubscribersCount(),
                isSubscribed
        );
    }

    private UserProfileEntity requireUserProfile(UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenOperationException("Current user is not authenticated");
        }
        return userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
    }
}
