package podcastService.playlist.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.common.dto.PageResponse;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.playlist.service.PlaylistService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/library/playlists")
@PreAuthorize("isAuthenticated()")
public class UserPlaylistLibraryController {

    private final PlaylistService playlistService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PlaylistCard> getSavedPlaylists(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        log.info("GET /users/me/library/playlists, currentUserId={}, page={}, size={}",
                currentUser.userId(), page, size);
        return playlistService.listSaved(currentUser.userId(), page, size);
    }
}
