package podcastService.history.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.common.dto.PageResponse;
import podcastService.history.dto.ListenHistoryItem;
import podcastService.history.dto.SaveProgressRequest;
import podcastService.history.service.ListenHistoryService;
import podcastService.infrastructure.security.AuthenticatedUser;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class ListenHistoryController {

    private final ListenHistoryService listenHistoryService;

    @GetMapping("/users/me/history")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<ListenHistoryItem> getMyHistory(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("GET /users/me/history, currentUserId={}, page={}, size={}", currentUserId, page, size);
        return listenHistoryService.listMine(currentUserId, page, size);
    }

    @PostMapping("/podcasts/{podcastId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveProgress(
            @PathVariable UUID podcastId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SaveProgressRequest request
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "POST /podcasts/{}/progress, currentUserId={}, progressSeconds={}",
                podcastId,
                currentUserId,
                request.progressSeconds()
        );
        listenHistoryService.saveProgress(podcastId, currentUserId, request);
        return ResponseEntity.noContent().build();
    }
}
