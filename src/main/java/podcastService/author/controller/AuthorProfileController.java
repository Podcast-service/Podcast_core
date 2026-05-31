package podcastService.author.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import podcastService.author.dto.*;
import podcastService.author.service.AuthorProfileService;
import podcastService.author.service.BecomeAuthorResult;
import podcastService.author.service.BecomeAuthorService;
import podcastService.common.dto.PageResponse;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.entity.Status;
import podcastService.podcast.service.PodcastService;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authors")
public class AuthorProfileController {

    private final AuthorProfileService authorProfileService;
    private final BecomeAuthorService becomeAuthorService;
    private final PodcastService podcastService;

    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BecomeAuthorResponse> createMyAuthorProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody CreateAuthorProfileRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "POST /authors/me author onboarding, currentUserId={}, authorNameLength={}, hasDescription={}",
                currentUserId,
                request.authorName() == null ? null : request.authorName().length(),
                request.description() != null
        );

        BecomeAuthorResult result = becomeAuthorService.becomeAuthor(currentUserId, authorizationHeader, request);
        HttpStatus status = result.authorProfileCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('AUTHOR')")
    @ResponseStatus(HttpStatus.OK)
    public AuthorProfileResponse getMyAuthorProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("GET /authors/me, currentUserId={}", currentUserId);
        return authorProfileService.getMine(currentUserId);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('AUTHOR')")
    @ResponseStatus(HttpStatus.OK)
    public AuthorProfileResponse updateMyAuthorProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateAuthorProfileRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "PUT /authors/me, currentUserId={}, fieldsChanged=[authorName:{}, description:{}]",
                currentUserId,
                request.isAuthorNameSet(),
                request.isDescriptionSet()
        );
        return authorProfileService.updateMine(currentUserId, request);
    }

    @GetMapping("/me/podcasts")
    @PreAuthorize("hasRole('AUTHOR')")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PodcastDetailResponse> getMyAuthorPodcasts(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SortPodcasts sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "GET /authors/me/podcasts, currentUserId={}, status={}, q='{}', sort={}, page={}, size={}",
                currentUserId,
                status,
                q,
                sort,
                page,
                size
        );
        return podcastService.listMineAsAuthor(currentUserId, status, q, sort, page, size);
    }

    @GetMapping("/{authorId}")
    @ResponseStatus(HttpStatus.OK)
    public AuthorProfileResponse getAuthorProfile(
            @PathVariable UUID authorId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info("GET /authors/{}, currentUserId={}", authorId, currentUserId);
        return authorProfileService.getPublic(authorId, currentUserId);
    }

    @GetMapping("/{authorId}/podcasts")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PodcastCard> getAuthorPodcasts(
            @PathVariable UUID authorId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SortPodcasts sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info(
                "GET /authors/{}/podcasts, currentUserId={}, q='{}', sort={}, page={}, size={}",
                authorId,
                currentUserId,
                q,
                sort,
                page,
                size
        );
        return podcastService.listByAuthor(authorId, q, sort, page, size, currentUserId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<AuthorCard> getAuthors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AuthorSort sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info("GET /authors, q='{}', sort={}, page={}, size={}, currentUserId={}",
                q, sort, page, size, currentUserId);
        return authorProfileService.getAuthors(new AuthorFilter(q, sort, page, size), currentUserId);
    }

    private UUID userIdOrNull(AuthenticatedUser currentUser) {
        return currentUser == null ? null : currentUser.userId();
    }
}
