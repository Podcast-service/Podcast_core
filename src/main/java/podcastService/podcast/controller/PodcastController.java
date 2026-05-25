package podcastService.podcast.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import podcastService.common.dto.PageResponse;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.podcast.dto.CreatePodcastRequest;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.dto.PodcastFilter;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.PodcastSpeakersResponse;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.dto.UpdatePodcastRequest;
import podcastService.podcast.service.PodcastService;
import podcastService.podcast.service.PodcastVoteService;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.dto.PodcastTranscriptResponse;
import podcastService.transcript.service.PodcastMediaService;
import podcastService.vote.dto.VoteRequest;
import podcastService.vote.dto.VoteResponse;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/podcasts")
public class PodcastController {

    private final PodcastService podcastService;
    private final PodcastVoteService podcastVoteService;
    private final PodcastMediaService podcastMediaService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PodcastCard> listPodcasts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) SortPodcasts sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        PodcastFilter filter = new PodcastFilter(q, categoryId, authorId, sort, page, size);
        UUID currentUserId = userIdOrNull(currentUser);

        log.info(
                "GET /podcasts, q='{}', categoryId={}, authorId={}, sort={}, page={}, size={}, currentUserId={}",
                q, categoryId, authorId, sort, page, size, currentUserId
        );

        return podcastService.list(filter, currentUserId);
    }

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<PodcastDetailResponse> createPodcast(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreatePodcastRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "POST /podcasts, currentUserId={}, categoryId={}, numSpeakers={}, titleLength={}",
                currentUserId,
                request.categoryId(),
                request.numSpeakers(),
                request.title() == null ? null : request.title().length()
        );

        PodcastDetailResponse response = podcastService.create(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{podcastId}")
    @ResponseStatus(HttpStatus.OK)
    public PodcastDetailResponse getPodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info("GET /podcasts/{}, currentUserId={}", podcastId, currentUserId);
        return podcastService.getById(podcastId, currentUserId);
    }

    @GetMapping("/{podcastId}/speakers")
    @ResponseStatus(HttpStatus.OK)
    public PodcastSpeakersResponse getPodcastSpeakers(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info("GET /podcasts/{}/speakers, currentUserId={}", podcastId, currentUserId);
        return podcastService.getSpeakersById(podcastId, currentUserId);
    }

    @PutMapping("/{podcastId}")
    @PreAuthorize("hasRole('AUTHOR')")
    @ResponseStatus(HttpStatus.OK)
    public PodcastDetailResponse updatePodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdatePodcastRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "PUT /podcasts/{}, currentUserId={}, fieldsChanged=[title:{}, description:{}, categoryId:{}, coverImageUrl:{}]",
                podcastId,
                currentUserId,
                request.isTitleSet(),
                request.isDescriptionSet(),
                request.isCategoryIdSet(),
                request.isCoverImageUrlSet()
        );

        return podcastService.update(podcastId, currentUserId, request);
    }

    @DeleteMapping("/{podcastId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<Void> deletePodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /podcasts/{}, currentUserId={}", podcastId, currentUserId);
        podcastService.archive(podcastId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{podcastId}/publish")
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<PodcastDetailResponse> publishPodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /podcasts/{}/publish, currentUserId={}", podcastId, currentUserId);
        PodcastDetailResponse response = podcastService.publish(podcastId, currentUserId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{podcastId}/transcript")
    @ResponseStatus(HttpStatus.OK)
    public PodcastTranscriptResponse getPodcastTranscript(@PathVariable UUID podcastId) {
        log.info("GET /podcasts/{}/transcript", podcastId);
        return podcastMediaService.getTranscript(podcastId);
    }

    @GetMapping("/{podcastId}/summary")
    @ResponseStatus(HttpStatus.OK)
    public PodcastSummaryResponse getPodcastSummary(@PathVariable UUID podcastId) {
        log.info("GET /podcasts/{}/summary", podcastId);
        return podcastMediaService.getSummary(podcastId);
    }

    @PostMapping("/{podcastId}/vote")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public VoteResponse votePodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody VoteRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /podcasts/{}/vote, currentUserId={}, voteType={}",
                podcastId, currentUserId, request.voteType());
        return podcastVoteService.vote(podcastId, currentUserId, request);
    }

    @DeleteMapping("/{podcastId}/vote")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public VoteResponse removeVotePodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /podcasts/{}/vote, currentUserId={}", podcastId, currentUserId);
        return podcastVoteService.removeVote(podcastId, currentUserId);
    }

    private UUID userIdOrNull(AuthenticatedUser currentUser) {
        return currentUser == null ? null : currentUser.userId();
    }
}
