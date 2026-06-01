package podcastService.podcast.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.common.exception.ForbiddenOperationException;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.outbox.recommendation.PodcastActivityEventFactory;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.PodcastVoteId;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteRequest;
import podcastService.vote.dto.VoteResponse;
import podcastService.vote.dto.VoteType;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastVoteService {

    private final PodcastRepository podcastRepository;
    private final PodcastVoteRepository podcastVoteRepository;
    private final UserProfileRepository userProfileRepository;
    private final RecommendationOutboxEventService recommendationOutboxEventService;

    @Transactional
    public VoteResponse vote(UUID podcastId, UUID currentUserId, VoteRequest request) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        PodcastEntity podcast = requirePublishedPodcastForUpdate(podcastId);

        PodcastVoteEntity vote = podcastVoteRepository
                .findByIdUserProfileIdAndIdPodcastId(currentUser.getId(), podcastId)
                .orElse(null);

        boolean changedToLike = false;
        boolean changedToDislike = false;
        if (vote == null) {
            vote = new PodcastVoteEntity();
            vote.setId(new PodcastVoteId(currentUser.getId(), podcastId));
            vote.setUserProfile(currentUser);
            vote.setPodcast(podcast);
            vote.setVoteType(request.voteType());
            applyVoteDelta(podcast, null, request.voteType());
            podcastVoteRepository.save(vote);
            changedToLike = request.voteType() == VoteType.LIKE;
            changedToDislike = request.voteType() == VoteType.DISLIKE;
        } else if (vote.getVoteType() != request.voteType()) {
            VoteType previousVote = vote.getVoteType();
            vote.setVoteType(request.voteType());
            applyVoteDelta(podcast, previousVote, request.voteType());
            changedToLike = request.voteType() == VoteType.LIKE;
            changedToDislike = request.voteType() == VoteType.DISLIKE;
        }

        PodcastEntity saved = podcastRepository.saveAndFlush(podcast);
        podcastVoteRepository.flush();
        savePodcastVoteOutboxEvent(saved, currentUserId, changedToLike, changedToDislike);

        log.info(
                "Podcast vote saved: podcastId={}, userId={}, voteType={}",
                podcastId,
                currentUserId,
                request.voteType()
        );

        return toResponse(saved, request.voteType());
    }

    @Transactional
    public VoteResponse removeVote(UUID podcastId, UUID currentUserId) {
        UserProfileEntity currentUser = requireUserProfile(currentUserId);
        PodcastEntity podcast = requirePublishedPodcastForUpdate(podcastId);

        PodcastVoteEntity vote = podcastVoteRepository
                .findByIdUserProfileIdAndIdPodcastId(currentUser.getId(), podcastId)
                .orElse(null);

        if (vote != null) {
            applyVoteDelta(podcast, vote.getVoteType(), null);
            podcastVoteRepository.delete(vote);
            podcastVoteRepository.flush();
            podcastRepository.saveAndFlush(podcast);
        }

        log.info("Podcast vote removed: podcastId={}, userId={}", podcastId, currentUserId);
        return toResponse(podcast, null);
    }

    private PodcastEntity requirePublishedPodcastForUpdate(UUID podcastId) {
        PodcastEntity podcast = podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        if (podcast.getStatus() != Status.PUBLISHED) {
            throw new NotFoundException("Podcast not found");
        }

        return podcast;
    }

    private UserProfileEntity requireUserProfile(UUID currentUserId) {
        if (currentUserId == null) {
            throw new ForbiddenOperationException("Current user is not authenticated");
        }

        return userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("User profile not found"));
    }

    private void applyVoteDelta(PodcastEntity podcast, VoteType previousVote, VoteType nextVote) {
        if (previousVote == VoteType.LIKE) {
            podcast.setLikesCount(Math.max(0, podcast.getLikesCount() - 1));
        } else if (previousVote == VoteType.DISLIKE) {
            podcast.setDislikesCount(Math.max(0, podcast.getDislikesCount() - 1));
        }

        if (nextVote == VoteType.LIKE) {
            podcast.setLikesCount(podcast.getLikesCount() + 1);
        } else if (nextVote == VoteType.DISLIKE) {
            podcast.setDislikesCount(podcast.getDislikesCount() + 1);
        }
    }

    private void savePodcastVoteOutboxEvent(
            PodcastEntity podcast,
            UUID currentUserId,
            boolean changedToLike,
            boolean changedToDislike
    ) {
        Instant occurredAt = Instant.now();
        if (changedToLike) {
            recommendationOutboxEventService.saveUserActivityEvent(
                    currentUserId,
                    PodcastActivityEventFactory.liked(
                            podcast.getId(),
                            currentUserId,
                            occurredAt,
                            null,
                            null
                    )
            );
        }

        if (changedToDislike) {
            recommendationOutboxEventService.saveUserActivityEvent(
                    currentUserId,
                    PodcastActivityEventFactory.disliked(
                            podcast.getId(),
                            currentUserId,
                            occurredAt,
                            null,
                            null
                    )
            );
        }
    }

    private VoteResponse toResponse(PodcastEntity podcast, VoteType currentUserVote) {
        return new VoteResponse(
                podcast.getId(),
                "PODCAST",
                podcast.getLikesCount(),
                podcast.getDislikesCount(),
                currentUserVote
        );
    }
}
