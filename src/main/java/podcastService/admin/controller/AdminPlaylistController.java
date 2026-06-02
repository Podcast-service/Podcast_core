package podcastService.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminPlaylistDetailResponse;
import podcastService.admin.dto.AdminPlaylistFilter;
import podcastService.admin.dto.AdminPlaylistResponse;
import podcastService.admin.service.AdminPlaylistService;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.playlist.dto.SortPlaylists;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/playlists")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPlaylistController {

    private final AdminPlaylistService adminPlaylistService;

    @GetMapping
    public AdminPageResponse<AdminPlaylistResponse> getPlaylists(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID ownerProfileId,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SortPlaylists sort
    ) {
        log.info("GET /admin/playlists, ownerProfileId={}, isPublic={}, page={}, size={}",
                ownerProfileId, isPublic, page, size);
        return adminPlaylistService.getPlaylists(new AdminPlaylistFilter(
                q,
                ownerProfileId,
                isPublic,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/{playlistId}")
    public AdminPlaylistDetailResponse getPlaylist(@PathVariable UUID playlistId) {
        log.info("GET /admin/playlists/{}", playlistId);
        return adminPlaylistService.getPlaylist(playlistId);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        log.info("DELETE /admin/playlists/{}, currentUserId={}", playlistId, currentUser.userId());
        adminPlaylistService.deletePlaylist(playlistId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
