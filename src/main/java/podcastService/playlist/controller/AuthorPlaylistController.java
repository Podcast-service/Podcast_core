package podcastService.playlist.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.common.dto.PageResponse;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.playlist.service.PlaylistService;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/authors/{authorId}/playlists")
public class AuthorPlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PlaylistCard> getAuthorPlaylists(
            @PathVariable UUID authorId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        UUID currentUserId = currentUser == null ? null : currentUser.userId();
        log.info("GET /authors/{}/playlists, currentUserId={}, page={}, size={}",
                authorId, currentUserId, page, size);
        return playlistService.listAuthorPlaylists(authorId, page, size, currentUserId);
    }
}
