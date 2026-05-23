package podcastService.search.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import podcastService.search.dto.SearchSort;
import podcastService.search.dto.SearchSuggestItem;
import podcastService.search.dto.SearchType;

import java.util.List;
import java.util.UUID;

@Repository
public class SearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<SearchSuggestItem> suggest(String queryText, int limit) {
        Query query = entityManager.createNativeQuery("""
                select type, id, label, cover_url
                from (
                    select 'PODCAST' as type, p.id, p.title as label, p.cover_image_url as cover_url, 1 as priority
                    from podcasts p
                    where p.status = 'PUBLISHED'
                      and lower(p.title) like lower(:prefix)

                    union all

                    select 'AUTHOR' as type, a.id, a.author_name as label, u.avatar_url as cover_url, 2 as priority
                    from author_profiles a
                    join user_profiles u on u.id = a.user_profile_id
                    where lower(a.author_name) like lower(:prefix)

                    union all

                    select 'PLAYLIST' as type, pl.id, pl.title as label, pl.cover_image_url as cover_url, 3 as priority
                    from playlists pl
                    where pl.is_public = true
                      and lower(pl.title) like lower(:prefix)
                ) suggestions
                order by priority asc, lower(label) asc, id asc
                limit :limit
                """);
        query.setParameter("prefix", queryText + "%");
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new SearchSuggestItem(
                        SearchType.valueOf(row[0].toString()),
                        toUuid(row[1]),
                        row[2].toString(),
                        row[3] == null ? null : row[3].toString()
                ))
                .toList();
    }

    public SearchIdPage searchPodcasts(String queryText, UUID categoryId, SearchSort sort, int page, int size) {
        String categoryFilter = categoryId == null ? "" : " and p.category_id = :categoryId";
        String where = """
                from podcasts p, websearch_to_tsquery('simple', normalize_search_text(:queryText)) query
                where p.status = 'PUBLISHED'
                """ + categoryFilter + """
                  and (
                        p.search_vector @@ query
                        or lower(p.title) like lower(:containsQuery)
                        or lower(coalesce(p.description, '')) like lower(:containsQuery)
                  )
                """;
        String orderBy = switch (sort) {
            case DATE -> "order by p.published_at desc nulls last, p.created_at desc, p.id desc";
            case RATING -> "order by (p.likes_count - p.dislikes_count) desc, p.likes_count desc, p.created_at desc, p.id desc";
            case VIEWS -> "order by p.views_count desc, p.likes_count desc, p.created_at desc, p.id desc";
            case RELEVANCE -> "order by ts_rank_cd(p.search_vector, query) desc, p.published_at desc nulls last, p.id desc";
        };

        return searchIds("select p.id " + where + " " + orderBy, "select count(*) " + where, queryText, categoryId, categoryId != null, page, size);
    }

    public SearchIdPage searchAuthors(String queryText, SearchSort sort, int page, int size) {
        String where = """
                from author_profiles a, websearch_to_tsquery('simple', normalize_search_text(:queryText)) query
                where a.search_vector @@ query
                   or lower(a.author_name) like lower(:containsQuery)
                   or lower(coalesce(a.description, '')) like lower(:containsQuery)
                """;
        String orderBy = switch (sort) {
            case DATE -> "order by a.created_at desc, a.id desc";
            case RATING, VIEWS -> "order by a.subscribers_count desc, a.created_at desc, a.id desc";
            case RELEVANCE -> "order by ts_rank_cd(a.search_vector, query) desc, a.subscribers_count desc, a.id desc";
        };

        return searchIds("select a.id " + where + " " + orderBy, "select count(*) " + where, queryText, null, false, page, size);
    }

    public SearchIdPage searchPlaylists(String queryText, SearchSort sort, int page, int size) {
        String where = """
                from playlists pl, websearch_to_tsquery('simple', normalize_search_text(:queryText)) query
                where pl.is_public = true
                  and (
                        pl.search_vector @@ query
                        or lower(pl.title) like lower(:containsQuery)
                        or lower(coalesce(pl.description, '')) like lower(:containsQuery)
                  )
                """;
        String orderBy = switch (sort) {
            case DATE -> "order by pl.created_at desc, pl.id desc";
            case RATING, VIEWS -> "order by (pl.likes_count - pl.dislikes_count) desc, pl.likes_count desc, pl.created_at desc, pl.id desc";
            case RELEVANCE -> "order by ts_rank_cd(pl.search_vector, query) desc, pl.created_at desc, pl.id desc";
        };

        return searchIds("select pl.id " + where + " " + orderBy, "select count(*) " + where, queryText, null, false, page, size);
    }

    private SearchIdPage searchIds(
            String selectSql,
            String countSql,
            String queryText,
            UUID categoryId,
            boolean bindCategory,
            int page,
            int size
    ) {
        Query selectQuery = bindSearchParameters(entityManager.createNativeQuery(selectSql), queryText, categoryId, bindCategory);
        selectQuery.setFirstResult((page - 1) * size);
        selectQuery.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object> rows = selectQuery.getResultList();
        List<UUID> ids = rows.stream().map(this::toUuid).toList();

        Query countQuery = bindSearchParameters(entityManager.createNativeQuery(countSql), queryText, categoryId, bindCategory);
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new SearchIdPage(ids, total);
    }

    private Query bindSearchParameters(Query query, String queryText, UUID categoryId, boolean bindCategory) {
        query.setParameter("queryText", queryText);
        query.setParameter("containsQuery", "%" + queryText + "%");
        if (bindCategory) {
            query.setParameter("categoryId", categoryId);
        }
        return query;
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
