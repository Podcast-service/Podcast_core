package podcastService.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.dto.AuthorCard;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.BadRequestException;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.mapper.PlaylistMapper;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.search.dto.SearchResponse;
import podcastService.search.dto.SearchSort;
import podcastService.search.dto.SearchSuggestItem;
import podcastService.search.dto.SearchType;
import podcastService.search.repository.SearchIdPage;
import podcastService.search.repository.SearchRepository;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int SUGGEST_LIMIT = 5;

    private final SearchRepository searchRepository;
    private final PodcastRepository podcastRepository;
    private final AuthorRepository authorRepository;
    private final PlaylistRepository playlistRepository;
    private final PodcastVoteRepository podcastVoteRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserProfileRepository userProfileRepository;
    private final PodcastMapper podcastMapper;
    private final PlaylistMapper playlistMapper;

    @Transactional(readOnly = true)
    public List<SearchSuggestItem> suggest(String queryText) {
        String normalizedQuery = normalizeQuery(queryText);
        List<SearchSuggestItem> suggestions = searchRepository.suggest(normalizedQuery, SUGGEST_LIMIT);
        log.debug("Search suggestions loaded: queryLength={}, items={}", normalizedQuery.length(), suggestions.size());
        return suggestions;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(
            String queryText,
            SearchType type,
            UUID categoryId,
            SearchSort sort,
            int page,
            int size,
            UUID currentUserId
    ) {
        String normalizedQuery = normalizeQuery(queryText);
        SearchType normalizedType = type == null ? SearchType.ALL : type;
        SearchSort normalizedSort = sort == null ? SearchSort.RELEVANCE : sort;
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 50);
        UUID currentUserProfileId = resolveUserProfileId(currentUserId);
        UUID currentAuthorId = resolveAuthorIdByUserId(currentUserId);

        PageResponse<PodcastCard> podcasts = shouldSearch(normalizedType, SearchType.PODCAST)
                ? searchPodcasts(normalizedQuery, categoryId, normalizedSort, normalizedPage, normalizedSize, currentUserProfileId, currentAuthorId)
                : emptyPage(normalizedPage, normalizedSize);
        PageResponse<AuthorCard> authors = shouldSearch(normalizedType, SearchType.AUTHOR)
                ? searchAuthors(normalizedQuery, normalizedSort, normalizedPage, normalizedSize, currentUserProfileId)
                : emptyPage(normalizedPage, normalizedSize);
        PageResponse<PlaylistCard> playlists = shouldSearch(normalizedType, SearchType.PLAYLIST)
                ? searchPlaylists(normalizedQuery, normalizedSort, normalizedPage, normalizedSize, currentUserProfileId)
                : emptyPage(normalizedPage, normalizedSize);

        log.debug(
                "Search completed: queryLength={}, type={}, sort={}, page={}, size={}, podcastTotal={}, authorTotal={}, playlistTotal={}",
                normalizedQuery.length(),
                normalizedType,
                normalizedSort,
                normalizedPage,
                normalizedSize,
                podcasts.meta().totalElements(),
                authors.meta().totalElements(),
                playlists.meta().totalElements()
        );

        return new SearchResponse(podcasts, authors, playlists);
    }

    private PageResponse<PodcastCard> searchPodcasts(
            String queryText,
            UUID categoryId,
            SearchSort sort,
            int page,
            int size,
            UUID currentUserProfileId,
            UUID currentAuthorId
    ) {
        SearchIdPage idPage = searchRepository.searchPodcasts(queryText, categoryId, sort, page, size);
        if (idPage.ids().isEmpty()) {
            return pageResponse(List.of(), page, size, idPage.totalElements());
        }

        List<PodcastEntity> podcasts = orderedByIds(idPage.ids(), podcastRepository.findDetailedByIdIn(idPage.ids()));
        Set<UUID> authorIds = podcasts.stream()
                .map(podcast -> podcast.getAuthor().getId())
                .collect(Collectors.toSet());
        Set<UUID> subscribedAuthorIds = currentUserProfileId == null || authorIds.isEmpty()
                ? null
                : subscriptionRepository.findSubscribedAuthorIds(currentUserProfileId, authorIds);
        Map<UUID, VoteType> currentUserVotes = resolveCurrentUserVotes(currentUserProfileId, idPage.ids());

        List<PodcastCard> items = podcasts.stream()
                .map(podcast -> podcastMapper.toCard(podcast, currentAuthorId, subscribedAuthorIds, currentUserVotes))
                .toList();
        return pageResponse(items, page, size, idPage.totalElements());
    }

    private PageResponse<AuthorCard> searchAuthors(
            String queryText,
            SearchSort sort,
            int page,
            int size,
            UUID currentUserProfileId
    ) {
        SearchIdPage idPage = searchRepository.searchAuthors(queryText, sort, page, size);
        if (idPage.ids().isEmpty()) {
            return pageResponse(List.of(), page, size, idPage.totalElements());
        }

        List<AuthorEntity> authors = orderedByIds(idPage.ids(), authorRepository.findDetailedByIdIn(idPage.ids()));
        Set<UUID> subscribedAuthorIds = currentUserProfileId == null || idPage.ids().isEmpty()
                ? null
                : subscriptionRepository.findSubscribedAuthorIds(currentUserProfileId, idPage.ids());

        List<AuthorCard> items = authors.stream()
                .map(author -> toAuthorCard(
                        author,
                        subscribedAuthorIds == null ? null : subscribedAuthorIds.contains(author.getId())
                ))
                .toList();
        return pageResponse(items, page, size, idPage.totalElements());
    }

    private PageResponse<PlaylistCard> searchPlaylists(
            String queryText,
            SearchSort sort,
            int page,
            int size,
            UUID currentUserProfileId
    ) {
        SearchIdPage idPage = searchRepository.searchPlaylists(queryText, sort, page, size);
        if (idPage.ids().isEmpty()) {
            return pageResponse(List.of(), page, size, idPage.totalElements());
        }

        List<PlaylistEntity> playlists = orderedByIds(idPage.ids(), playlistRepository.findWithOwnerByIdIn(idPage.ids()));
        List<PlaylistCard> items = playlists.stream()
                .map(playlist -> playlistMapper.toCard(playlist, currentUserProfileId))
                .toList();
        return pageResponse(items, page, size, idPage.totalElements());
    }

    private boolean shouldSearch(SearchType requestedType, SearchType targetType) {
        return requestedType == SearchType.ALL || requestedType == targetType;
    }

    private String normalizeQuery(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new BadRequestException("Search query must not be blank");
        }
        return queryText.trim();
    }

    private Map<UUID, VoteType> resolveCurrentUserVotes(UUID currentUserProfileId, Collection<UUID> podcastIds) {
        if (currentUserProfileId == null) {
            return null;
        }
        if (podcastIds.isEmpty()) {
            return Map.of();
        }
        return podcastVoteRepository.findByUserProfileIdAndPodcastIds(currentUserProfileId, podcastIds)
                .stream()
                .collect(Collectors.toMap(
                        vote -> vote.getId().getPodcastId(),
                        PodcastVoteEntity::getVoteType
                ));
    }

    private UUID resolveUserProfileId(UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return userProfileRepository.findByUserId(currentUserId)
                .map(UserProfileEntity::getId)
                .orElse(null);
    }

    private UUID resolveAuthorIdByUserId(UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return authorRepository.findByUserProfileUserId(currentUserId)
                .map(AuthorEntity::getId)
                .orElse(null);
    }

    private AuthorCard toAuthorCard(AuthorEntity author, Boolean isSubscribed) {
        return new AuthorCard(
                author.getId(),
                author.getAuthorName(),
                author.getUserProfile() == null ? null : author.getUserProfile().getAvatarUrl(),
                author.getSubscribersCount(),
                isSubscribed
        );
    }

    private <T> List<T> orderedByIds(List<UUID> ids, List<T> entities) {
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, T> byId = entities.stream()
                .collect(Collectors.toMap(this::entityId, Function.identity()));

        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private UUID entityId(Object entity) {
        if (entity instanceof PodcastEntity podcast) {
            return podcast.getId();
        }
        if (entity instanceof AuthorEntity author) {
            return author.getId();
        }
        if (entity instanceof PlaylistEntity playlist) {
            return playlist.getId();
        }
        throw new IllegalArgumentException("Unsupported search entity type: " + entity.getClass().getName());
    }

    private <T> PageResponse<T> pageResponse(List<T> items, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil(totalElements / (double) size);
        return new PageResponse<>(items, new PageMeta(page, size, totalElements, totalPages));
    }

    private <T> PageResponse<T> emptyPage(int page, int size) {
        return pageResponse(List.of(), page, size, 0);
    }
}
