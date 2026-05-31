package podcastService.playlist.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.common.dto.PageResponse;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.playlist.dto.AddPodcastToPlaylistRequest;
import podcastService.playlist.dto.CreatePlaylistRequest;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.playlist.dto.PlaylistDetailResponse;
import podcastService.playlist.dto.PlaylistFilter;
import podcastService.playlist.dto.PlaylistSaveResponse;
import podcastService.playlist.dto.ReorderPlaylistRequest;
import podcastService.playlist.dto.SortPlaylists;
import podcastService.playlist.dto.UpdatePlaylistRequest;
import podcastService.playlist.service.PlaylistService;
import podcastService.vote.dto.VoteRequest;
import podcastService.vote.dto.VoteResponse;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PlaylistCard> listPlaylists(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SortPlaylists sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info("GET /playlists, q='{}', sort={}, page={}, size={}, currentUserId={}",
                q, sort, page, size, currentUserId);
        return playlistService.listPublic(new PlaylistFilter(q, sort, page, size), currentUserId);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlaylistDetailResponse> createPlaylist(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreatePlaylistRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /playlists, currentUserId={}, titleLength={}, public={}",
                currentUserId,
                request.title() == null ? null : request.title().length(),
                request.isPublic());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.create(currentUserId, request));
    }

    @GetMapping("/{playlistId}")
    @ResponseStatus(HttpStatus.OK)
    public PlaylistDetailResponse getPlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = userIdOrNull(currentUser);
        log.info("GET /playlists/{}, currentUserId={}", playlistId, currentUserId);
        return playlistService.get(playlistId, currentUserId);
    }

    @PostMapping("/{playlistId}/save")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public PlaylistSaveResponse savePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /playlists/{}/save, currentUserId={}", playlistId, currentUserId);
        return playlistService.saveToLibrary(playlistId, currentUserId);
    }

    @DeleteMapping("/{playlistId}/save")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public PlaylistSaveResponse removeSavedPlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /playlists/{}/save, currentUserId={}", playlistId, currentUserId);
        return playlistService.removeFromLibrary(playlistId, currentUserId);
    }

    @PutMapping("/{playlistId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public PlaylistDetailResponse updatePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdatePlaylistRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "PUT /playlists/{}, currentUserId={}, fieldsChanged=[title:{}, description:{}, coverImageUrl:{}, isPublic:{}]",
                playlistId,
                currentUserId,
                request.isTitleSet(),
                request.isDescriptionSet(),
                request.isCoverImageUrlSet(),
                request.isPublicSet()
        );
        return playlistService.update(playlistId, currentUserId, request);
    }

    @DeleteMapping("/{playlistId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /playlists/{}, currentUserId={}", playlistId, currentUserId);
        playlistService.delete(playlistId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{playlistId}/podcasts")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public PlaylistDetailResponse addPodcastToPlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AddPodcastToPlaylistRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /playlists/{}/podcasts, currentUserId={}, podcastId={}",
                playlistId, currentUserId, request.podcastId());
        return playlistService.addPodcast(playlistId, currentUserId, request);
    }

    @DeleteMapping("/{playlistId}/podcasts/{podcastId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removePodcastFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /playlists/{}/podcasts/{}, currentUserId={}",
                playlistId, podcastId, currentUserId);
        playlistService.removePodcast(playlistId, podcastId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{playlistId}/podcasts/reorder")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public PlaylistDetailResponse reorderPlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ReorderPlaylistRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("PUT /playlists/{}/podcasts/reorder, currentUserId={}, items={}",
                playlistId, currentUserId, request.items() == null ? null : request.items().size());
        return playlistService.reorder(playlistId, currentUserId, request);
    }

    @PostMapping("/{playlistId}/vote")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public VoteResponse votePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody VoteRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /playlists/{}/vote, currentUserId={}, voteType={}",
                playlistId, currentUserId, request.voteType());
        return playlistService.vote(playlistId, currentUserId, request);
    }

    @DeleteMapping("/{playlistId}/vote")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public VoteResponse removeVotePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /playlists/{}/vote, currentUserId={}", playlistId, currentUserId);
        return playlistService.removeVote(playlistId, currentUserId);
    }

    private UUID userIdOrNull(AuthenticatedUser currentUser) {
        return currentUser == null ? null : currentUser.userId();
    }
}
