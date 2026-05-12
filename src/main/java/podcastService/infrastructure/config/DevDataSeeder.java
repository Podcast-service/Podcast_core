package podcastService.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final UUID DEV_PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID DEV_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DEV_AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    private static final UUID TECH_PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440101");
    private static final UUID TECH_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID TECH_AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440110");

    private static final UUID OPS_PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440201");
    private static final UUID OPS_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440200");
    private static final UUID OPS_AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440210");

    private static final UUID BACKEND_CATEGORY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655441001");
    private static final UUID PRODUCT_CATEGORY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655441002");

    private static final UUID KAFKA_PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655442001");
    private static final UUID SPRING_PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655442002");
    private static final UUID PRODUCT_PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655442003");

    private static final UUID DEV_PLAYLIST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655443001");
    private static final UUID PUBLIC_PLAYLIST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655443002");

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.dev-seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Dev data seed is disabled");
            return;
        }

        seedUsers();
        seedAuthors();
        seedCategories();
        seedPodcasts();
        seedPodcastMedia();
        seedPlaylists();
        seedSubscriptions();
        seedVotesAndHistory();

        log.info(
                "Dev data seed applied: defaultTokenUserId={}, defaultAuthorId={}, samplePodcastId={}",
                DEV_USER_ID,
                DEV_AUTHOR_ID,
                KAFKA_PODCAST_ID
        );
    }

    private void seedUsers() {
        upsertUserProfile(DEV_PROFILE_ID, DEV_USER_ID, "dev_listener", "https://cdn.example.com/dev/avatar.png");
        upsertUserProfile(TECH_PROFILE_ID, TECH_USER_ID, "tech_author", "https://cdn.example.com/dev/tech-author.png");
        upsertUserProfile(OPS_PROFILE_ID, OPS_USER_ID, "ops_author", "https://cdn.example.com/dev/ops-author.png");
    }

    private void seedAuthors() {
        upsertAuthor(DEV_AUTHOR_ID, DEV_PROFILE_ID, "Dev Author", "Авторский профиль пользователя из dev-токена.", 0);
        upsertAuthor(TECH_AUTHOR_ID, TECH_PROFILE_ID, "Backend Kitchen", "Практичные разговоры про Java, Kafka и прод.", 1);
        upsertAuthor(OPS_AUTHOR_ID, OPS_PROFILE_ID, "Product Ops", "Про продуктовую разработку, процессы и метрики.", 0);
    }

    private void seedCategories() {
        jdbcTemplate.update("""
                insert into categories (id, name, position)
                values (?, ?, ?)
                on conflict (id) do update
                   set name = excluded.name,
                       position = excluded.position
                """, BACKEND_CATEGORY_ID, "Backend", 10);
        jdbcTemplate.update("""
                insert into categories (id, name, position)
                values (?, ?, ?)
                on conflict (id) do update
                   set name = excluded.name,
                       position = excluded.position
                """, PRODUCT_CATEGORY_ID, "Product", 20);
    }

    private void seedPodcasts() {
        upsertPodcast(
                KAFKA_PODCAST_ID,
                TECH_AUTHOR_ID,
                BACKEND_CATEGORY_ID,
                "Как работает Kafka в проде",
                "Разбираем топики, consumer groups, ретраи, DLQ и наблюдаемость без магии.",
                "https://cdn.example.com/dev/covers/kafka-prod.jpg",
                "https://cdn.example.com/dev/audio/kafka-prod.mp3",
                1830,
                42,
                12,
                1
        );
        upsertPodcast(
                SPRING_PODCAST_ID,
                TECH_AUTHOR_ID,
                BACKEND_CATEGORY_ID,
                "Spring Boot сервис без боли",
                "Архитектура REST API, валидация, логирование, миграции и тестирование.",
                "https://cdn.example.com/dev/covers/spring-boot.jpg",
                "https://cdn.example.com/dev/audio/spring-boot.mp3",
                2460,
                25,
                9,
                0
        );
        upsertPodcast(
                PRODUCT_PODCAST_ID,
                OPS_AUTHOR_ID,
                PRODUCT_CATEGORY_ID,
                "Метрики, которые не обманывают",
                "Говорим про продуктовые метрики, качество решений и работу с гипотезами.",
                "https://cdn.example.com/dev/covers/product-metrics.jpg",
                "https://cdn.example.com/dev/audio/product-metrics.mp3",
                1980,
                17,
                7,
                0
        );
    }

    private void seedPodcastMedia() {
        upsertTranscript(KAFKA_PODCAST_ID, "RU", "Сегодня разбираем, как Kafka ведет себя в продовой нагрузке.");
        upsertSummary(KAFKA_PODCAST_ID, "RU", "Коротко: проектируйте топики осознанно, измеряйте lag и проверяйте ретраи.");
        upsertTranscript(SPRING_PODCAST_ID, "RU", "В выпуске обсуждаем чистую структуру Spring Boot микросервиса.");
        upsertSummary(SPRING_PODCAST_ID, "RU", "Главная мысль: валидация, ошибки и наблюдаемость должны быть частью дизайна API.");
    }

    private void seedPlaylists() {
        upsertPlaylist(DEV_PLAYLIST_ID, DEV_PROFILE_ID, "Мой backend-плейлист", "Плейлист пользователя из dev-токена.", true, 5, 0);
        upsertPlaylist(PUBLIC_PLAYLIST_ID, TECH_PROFILE_ID, "Backend essentials", "Базовый набор выпусков для backend-разработчика.", true, 14, 1);

        upsertPlaylistPodcast(DEV_PLAYLIST_ID, KAFKA_PODCAST_ID, 1);
        upsertPlaylistPodcast(DEV_PLAYLIST_ID, SPRING_PODCAST_ID, 2);
        upsertPlaylistPodcast(PUBLIC_PLAYLIST_ID, SPRING_PODCAST_ID, 1);
        upsertPlaylistPodcast(PUBLIC_PLAYLIST_ID, KAFKA_PODCAST_ID, 2);
    }

    private void seedSubscriptions() {
        jdbcTemplate.update("""
                insert into subscriptions (subscriber_profile_id, author_id)
                values (?, ?)
                on conflict (subscriber_profile_id, author_id) do nothing
                """, DEV_PROFILE_ID, TECH_AUTHOR_ID);
    }

    private void seedVotesAndHistory() {
        upsertPodcastVote(DEV_PROFILE_ID, KAFKA_PODCAST_ID, "LIKE");
        upsertPlaylistVote(DEV_PROFILE_ID, PUBLIC_PLAYLIST_ID, "LIKE");
        upsertListenHistory(DEV_PROFILE_ID, KAFKA_PODCAST_ID, 920, false);
        upsertListenHistory(DEV_PROFILE_ID, SPRING_PODCAST_ID, 2400, true);
    }

    private void upsertUserProfile(UUID id, UUID userId, String username, String avatarUrl) {
        jdbcTemplate.update("""
                insert into user_profiles (id, user_id, username, avatar_url)
                values (?, ?, ?, ?)
                on conflict (id) do update
                   set user_id = excluded.user_id,
                       username = excluded.username,
                       avatar_url = excluded.avatar_url
                """, id, userId, username, avatarUrl);
    }

    private void upsertAuthor(UUID id, UUID profileId, String authorName, String description, long subscribersCount) {
        jdbcTemplate.update("""
                insert into author_profiles (id, user_profile_id, author_name, description, subscribers_count)
                values (?, ?, ?, ?, ?)
                on conflict (id) do update
                   set user_profile_id = excluded.user_profile_id,
                       author_name = excluded.author_name,
                       description = excluded.description,
                       subscribers_count = excluded.subscribers_count
                """, id, profileId, authorName, description, subscribersCount);
    }

    private void upsertPodcast(
            UUID id,
            UUID authorId,
            UUID categoryId,
            String title,
            String description,
            String coverImageUrl,
            String audioUrl,
            int durationSeconds,
            long viewsCount,
            long likesCount,
            long dislikesCount
    ) {
        jdbcTemplate.update("""
                insert into podcasts (
                    id, author_id, category_id, title, description, cover_image_url, audio_url,
                    duration_seconds, status, views_count, likes_count, dislikes_count, published_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?, ?, now() - interval '2 days')
                on conflict (id) do update
                   set author_id = excluded.author_id,
                       category_id = excluded.category_id,
                       title = excluded.title,
                       description = excluded.description,
                       cover_image_url = excluded.cover_image_url,
                       audio_url = excluded.audio_url,
                       duration_seconds = excluded.duration_seconds,
                       status = excluded.status,
                       views_count = excluded.views_count,
                       likes_count = excluded.likes_count,
                       dislikes_count = excluded.dislikes_count,
                       published_at = excluded.published_at
                """, id, authorId, categoryId, title, description, coverImageUrl, audioUrl,
                durationSeconds, viewsCount, likesCount, dislikesCount);
    }

    private void upsertTranscript(UUID podcastId, String language, String content) {
        jdbcTemplate.update("""
                insert into podcast_transcripts (podcast_id, language, content)
                values (?, ?, ?)
                on conflict (podcast_id, language) do update
                   set content = excluded.content
                """, podcastId, language, content);
    }

    private void upsertSummary(UUID podcastId, String language, String content) {
        jdbcTemplate.update("""
                insert into podcast_summaries (podcast_id, language, content)
                values (?, ?, ?)
                on conflict (podcast_id, language) do update
                   set content = excluded.content
                """, podcastId, language, content);
    }

    private void upsertPlaylist(UUID id, UUID ownerProfileId, String title, String description, boolean isPublic, long likes, long dislikes) {
        jdbcTemplate.update("""
                insert into playlists (id, owner_profile_id, title, description, cover_image_url, is_public, likes_count, dislikes_count)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update
                   set owner_profile_id = excluded.owner_profile_id,
                       title = excluded.title,
                       description = excluded.description,
                       cover_image_url = excluded.cover_image_url,
                       is_public = excluded.is_public,
                       likes_count = excluded.likes_count,
                       dislikes_count = excluded.dislikes_count
                """, id, ownerProfileId, title, description, "https://cdn.example.com/dev/covers/backend-playlist.jpg", isPublic, likes, dislikes);
    }

    private void upsertPlaylistPodcast(UUID playlistId, UUID podcastId, int position) {
        jdbcTemplate.update("""
                insert into playlist_podcasts (playlist_id, podcast_id, position)
                values (?, ?, ?)
                on conflict (playlist_id, podcast_id) do update
                   set position = excluded.position
                """, playlistId, podcastId, position);
    }

    private void upsertPodcastVote(UUID userProfileId, UUID podcastId, String voteType) {
        jdbcTemplate.update("""
                insert into podcast_votes (user_profile_id, podcast_id, vote_type)
                values (?, ?, ?)
                on conflict (user_profile_id, podcast_id) do update
                   set vote_type = excluded.vote_type
                """, userProfileId, podcastId, voteType);
    }

    private void upsertPlaylistVote(UUID userProfileId, UUID playlistId, String voteType) {
        jdbcTemplate.update("""
                insert into playlist_votes (user_profile_id, playlist_id, vote_type)
                values (?, ?, ?)
                on conflict (user_profile_id, playlist_id) do update
                   set vote_type = excluded.vote_type
                """, userProfileId, playlistId, voteType);
    }

    private void upsertListenHistory(UUID userProfileId, UUID podcastId, int progressSeconds, boolean completed) {
        jdbcTemplate.update("""
                insert into listen_history (user_profile_id, podcast_id, progress_seconds, completed)
                values (?, ?, ?, ?)
                on conflict (user_profile_id, podcast_id) do update
                   set progress_seconds = excluded.progress_seconds,
                       completed = excluded.completed
                """, userProfileId, podcastId, progressSeconds, completed);
    }
}
