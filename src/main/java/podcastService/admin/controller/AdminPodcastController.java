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
import podcastService.admin.dto.AdminPodcastFilter;
import podcastService.admin.dto.AdminPodcastResponse;
import podcastService.admin.service.AdminPodcastService;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.entity.Status;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/podcasts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPodcastController {

    private final AdminPodcastService adminPodcastService;

    @GetMapping
    public AdminPageResponse<AdminPodcastResponse> getPodcasts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SortPodcasts sort
    ) {
        log.info("GET /admin/podcasts, status={}, authorId={}, categoryId={}, page={}, size={}",
                status, authorId, categoryId, page, size);
        return adminPodcastService.getPodcasts(new AdminPodcastFilter(
                q,
                status,
                authorId,
                categoryId,
                page,
                size,
                sort
        ));
    }

    @GetMapping("/{podcastId}")
    public AdminPodcastResponse getPodcast(@PathVariable UUID podcastId) {
        log.info("GET /admin/podcasts/{}", podcastId);
        return adminPodcastService.getPodcast(podcastId);
    }

    @DeleteMapping("/{podcastId}")
    public ResponseEntity<Void> deletePodcast(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        log.info("DELETE /admin/podcasts/{}, currentUserId={}", podcastId, currentUser.userId());
        adminPodcastService.deletePodcast(podcastId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
