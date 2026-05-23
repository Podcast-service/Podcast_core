package podcastService.search.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.search.dto.SearchResponse;
import podcastService.search.dto.SearchSort;
import podcastService.search.dto.SearchSuggestItem;
import podcastService.search.dto.SearchType;
import podcastService.search.service.SearchService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search/suggest")
    @ResponseStatus(HttpStatus.OK)
    public List<SearchSuggestItem> suggest(
            @RequestParam @Size(min = 1, message = "q must contain at least 1 character") String q
    ) {
        log.info("GET /search/suggest, queryLength={}", q == null ? null : q.trim().length());
        return searchService.suggest(q);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public SearchResponse search(
            @RequestParam @Size(min = 1, message = "q must contain at least 1 character") String q,
            @RequestParam(required = false) SearchType type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) SearchSort sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser == null ? null : currentUser.userId();
        log.info(
                "GET /search, queryLength={}, type={}, categoryId={}, sort={}, page={}, size={}, currentUserId={}",
                q == null ? null : q.trim().length(),
                type,
                categoryId,
                sort,
                page,
                size,
                currentUserId
        );
        return searchService.search(q, type, categoryId, sort, page, size, currentUserId);
    }
}
