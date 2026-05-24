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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final UUID DEV_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEV_AUTHOR_ID = uuid("author", 1);

    private static final String[] CATEGORY_NAMES = {
            "Backend", "Frontend", "Architecture", "Data", "DevOps", "Product",
            "Design", "Security", "Mobile", "Career", "AI Lab", "QA"
    };

    private static final String[] AUTHOR_NAMES = {
            "Dev Author", "Backend Kitchen", "Product Ops", "Frontend Waves", "Data Stories",
            "Architecture Notes", "QA Garage", "Security Room", "Mobile People", "Design Critique",
            "AI Lab Local", "Career Debug", "Cloud Native Talks", "Startup Diary"
    };

    private static final String[] AUTHOR_DESCRIPTIONS = {
            "Авторский профиль пользователя из dev-токена. Подходит для проверки защищённых ручек и UI сценариев.",
            "Практичные разговоры про Java, Kafka, Spring Boot и эксплуатацию микросервисов.",
            "Про продуктовую разработку, метрики, discovery, delivery и спокойную работу с гипотезами.",
            "Подкаст о современных frontend-подходах, дизайн-системах, браузерах и DX.",
            "Истории про данные: аналитика, ETL, warehouse, BI и честные dashboard-практики.",
            "Архитектурные заметки о сложных системах, компромиссах и границах сервисов.",
            "QA-практики, автотесты, exploratory testing и культура качества без формализма.",
            "Безопасность приложений, JWT, угрозы, эксплуатационные риски и secure by design.",
            "Мобильная разработка, релизы, performance, offline-first и команды продукта.",
            "Дизайн интерфейсов, UX research, тексты, визуальная система и продуктовая ясность.",
            "Локальная AI-лаборатория: модели, пайплайны, оценка качества и прикладные кейсы.",
            "Карьера инженера, рост, коммуникация, собеседования и здоровый рабочий ритм.",
            "Cloud native, Kubernetes, observability, инциденты и инфраструктурные решения.",
            "Дневник стартапа: решения, ошибки, фокус, продажи и технический долг."
    };

    private static final String[] TOPICS = {
            "Kafka в проде", "Spring Boot без боли", "REST API для фронтенда",
            "Поиск и ранжирование", "JWT и роли", "Миграции PostgreSQL",
            "Observability для команды", "Дизайн ошибок API", "Плейлисты и UX",
            "Лента подписок", "Архитектура монолита и сервисов", "Feature flags",
            "Performance tuning", "Тестовые данные", "Security review", "CI-less локальная разработка",
            "Product analytics", "Mobile release flow", "QA strategy", "AI ассистенты"
    };

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.dev-seed.enabled:true}")
    private boolean enabled;

    private List<UserSeed> users;
    private List<AuthorSeed> authors;
    private List<UUID> categories;
    private List<PodcastSeed> podcasts;
    private List<UUID> playlists;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Dev data seed is disabled");
            return;
        }

        users = buildUsers();
        authors = buildAuthors(users);
        categories = seedCategories();
        podcasts = seedPodcasts(authors, categories);
        seedPodcastMedia(podcasts);
        playlists = seedPlaylists(users, podcasts);
        seedSubscriptions(users, authors);
        seedVotesAndHistory(users, podcasts, playlists);
        refreshCounters();

        log.info(
                "Dev data seed applied: users={}, authors={}, categories={}, podcasts={}, playlists={}, tokenUserId={}",
                users.size(),
                authors.size(),
                categories.size(),
                podcasts.size(),
                playlists.size(),
                DEV_USER_ID
        );
    }

    private List<UserSeed> buildUsers() {
        List<UserSeed> result = new ArrayList<>();
        result.add(new UserSeed(DEV_PROFILE_ID, DEV_USER_ID, "dev-user", "https://cdn.example.com/dev/users/dev-user.png"));

        String[] names = {
                "qa_reader", "frontend_olga", "backend_max", "mobile_ivan", "designer_anna",
                "data_mira", "ops_roman", "security_lee", "product_nika", "tester_pavel",
                "student_kira", "lead_alex", "founder_mila", "android_tim", "ios_sonya",
                "ux_lena", "devrel_mike", "architect_yuri", "analyst_dina", "scrum_egor",
                "java_sasha", "react_vika", "go_artem", "python_liza", "kotlin_oleg",
                "cloud_irina", "support_noah", "writer_maya", "mentor_igor", "intern_tanya",
                "admin_local", "author_local", "listener_edge", "unicode_юзер", "empty_avatar"
        };

        for (int i = 0; i < names.length; i++) {
            String avatar = i == names.length - 1 ? null : "https://cdn.example.com/dev/users/" + names[i] + ".png";
            result.add(new UserSeed(uuid("profile", i + 2), uuid("user", i + 2), names[i], avatar));
        }

        for (UserSeed user : result) {
            upsertUserProfile(user.profileId(), user.userId(), user.username(), user.avatarUrl());
        }

        return result;
    }

    private List<AuthorSeed> buildAuthors(List<UserSeed> userSeeds) {
        List<AuthorSeed> result = new ArrayList<>();

        for (int i = 0; i < AUTHOR_NAMES.length; i++) {
            UserSeed owner = userSeeds.get(i);
            UUID authorId = existingUuid(
                    "select id from author_profiles where user_profile_id = ?",
                    owner.profileId()
            );
            if (authorId == null) {
                authorId = i == 0 ? DEV_AUTHOR_ID : uuid("author", i + 1);
            }
            AuthorSeed author = new AuthorSeed(authorId, owner.profileId(), AUTHOR_NAMES[i], AUTHOR_DESCRIPTIONS[i]);
            result.add(author);
            upsertAuthor(author.id(), author.profileId(), author.name(), author.description());
        }

        return result;
    }

    private List<UUID> seedCategories() {
        List<UUID> result = new ArrayList<>();
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            UUID id = existingUuid("select id from categories where lower(name) = lower(?)", CATEGORY_NAMES[i]);
            if (id == null) {
                id = uuid("category", i + 1);
            }
            result.add(id);
            upsertCategory(id, CATEGORY_NAMES[i], 1000 + (i + 1) * 10);
        }
        return result;
    }

    private List<PodcastSeed> seedPodcasts(List<AuthorSeed> authorSeeds, List<UUID> categoryIds) {
        List<PodcastSeed> result = new ArrayList<>();
        int index = 1;

        for (int authorIndex = 0; authorIndex < authorSeeds.size(); authorIndex++) {
            AuthorSeed author = authorSeeds.get(authorIndex);
            for (int episode = 1; episode <= 9; episode++) {
                String status = statusFor(authorIndex, episode);
                boolean publishedLike = status.equals("PUBLISHED") || status.equals("ARCHIVED");
                int topicIndex = (authorIndex * 3 + episode) % TOPICS.length;
                UUID podcastId = uuid("podcast", index);
                UUID categoryId = categoryIds.get((authorIndex + episode) % categoryIds.size());
                int duration = publishedLike ? 480 + ((authorIndex + 1) * 137 + episode * 211) % 7200 : 0;
                String title = buildPodcastTitle(author.name(), TOPICS[topicIndex], episode, status);
                String description = buildPodcastDescription(author.name(), TOPICS[topicIndex], episode, status);
                String cover = episode % 7 == 0 ? null : "https://cdn.example.com/dev/covers/podcast-" + index + ".jpg";
                String audio = publishedLike ? "https://cdn.example.com/dev/audio/podcast-" + index + ".mp3" : null;
                String audioFile = publishedLike ? "/dev/audio/podcast-" + index + ".mp3" : null;
                long audioSizeFile = publishedLike ? 5_000_000L + (long) duration * 220L : 0L;
                int numSpeakers = 1 + ((authorIndex + episode) % 5);
                long views = publishedLike ? 25L + (long) authorIndex * 43L + episode * 17L : 0L;
                long likes = publishedLike ? 3L + (authorIndex + episode) % 31 : 0L;
                long dislikes = publishedLike ? (authorIndex + episode) % 5 : 0L;
                int daysAgo = 2 + index;

                PodcastSeed podcast = new PodcastSeed(
                        podcastId, author.id(), categoryId, title, description, cover, audio,
                        audioFile, publishedLike ? audioSizeFile : null, numSpeakers,
                        duration == 0 ? null : duration, status, views, likes, dislikes, daysAgo
                );
                result.add(podcast);
                upsertPodcast(podcast);
                index++;
            }
        }

        return result;
    }

    private void seedPodcastMedia(List<PodcastSeed> podcastSeeds) {
        for (PodcastSeed podcast : podcastSeeds) {
            if (!podcast.status().equals("PUBLISHED")) {
                continue;
            }
            String transcript = "В этом выпуске " + podcast.title()
                    + " ведущие обсуждают практику, ограничения и реальные сценарии внедрения. "
                    + "Текст содержит кириллицу, latin words, edge cases и фразы для проверки поиска.";
            String summary = "Коротко: " + podcast.title()
                    + " помогает проверить карточки, детали, transcript, summary и поиск в dev-интерфейсе.";
            upsertTranscript(podcast.id(), "RU", transcript);
            upsertSummary(podcast.id(), "RU", summary);

            if (Math.abs(podcast.id().hashCode()) % 5 == 0) {
                upsertTranscript(podcast.id(), "EN", "English transcript sample for " + podcast.title() + ".");
                upsertSummary(podcast.id(), "EN", "English summary sample for frontend language edge cases.");
            }
        }
    }

    private List<UUID> seedPlaylists(List<UserSeed> userSeeds, List<PodcastSeed> podcastSeeds) {
        List<UUID> result = new ArrayList<>();
        List<PodcastSeed> published = podcastSeeds.stream()
                .filter(podcast -> podcast.status().equals("PUBLISHED"))
                .toList();
        int index = 1;

        for (int userIndex = 0; userIndex < userSeeds.size(); userIndex++) {
            UserSeed owner = userSeeds.get(userIndex);
            for (int playlistNumber = 1; playlistNumber <= 2; playlistNumber++) {
                UUID playlistId = uuid("playlist", index);
                boolean isPublic = playlistNumber == 1 || userIndex % 3 != 0;
                String title = (playlistNumber == 1 ? "Слушаю сейчас: " : "Deep dive: ") + owner.username();
                String description = playlistNumber == 1
                        ? "Подборка для проверки пользовательских плейлистов, сортировки и голосов."
                        : "Длинное описание dev-плейлиста с несколькими темами: backend, product, QA, security и UI.";
                String cover = userIndex % 6 == 0 ? null : "https://cdn.example.com/dev/covers/playlist-" + index + ".jpg";
                upsertPlaylist(playlistId, owner.profileId(), title, description, cover, isPublic, 0, 0);
                clearPlaylistItems(playlistId);

                for (int position = 1; position <= 8; position++) {
                    PodcastSeed podcast = published.get((userIndex * 5 + playlistNumber * 7 + position) % published.size());
                    upsertPlaylistPodcast(playlistId, podcast.id(), position);
                }

                result.add(playlistId);
                index++;
            }
        }

        return result;
    }

    private void seedSubscriptions(List<UserSeed> userSeeds, List<AuthorSeed> authorSeeds) {
        for (int userIndex = 0; userIndex < userSeeds.size(); userIndex++) {
            UserSeed user = userSeeds.get(userIndex);
            for (int offset = 1; offset <= 5; offset++) {
                AuthorSeed author = authorSeeds.get((userIndex + offset) % authorSeeds.size());
                if (!author.profileId().equals(user.profileId())) {
                    upsertSubscription(user.profileId(), author.id());
                }
            }
        }
    }

    private void seedVotesAndHistory(List<UserSeed> userSeeds, List<PodcastSeed> podcastSeeds, List<UUID> playlistIds) {
        List<PodcastSeed> published = podcastSeeds.stream()
                .filter(podcast -> podcast.status().equals("PUBLISHED"))
                .toList();

        for (int userIndex = 0; userIndex < userSeeds.size(); userIndex++) {
            UserSeed user = userSeeds.get(userIndex);

            for (int voteIndex = 0; voteIndex < Math.min(18, published.size()); voteIndex++) {
                PodcastSeed podcast = published.get((userIndex * 7 + voteIndex) % published.size());
                String voteType = (userIndex + voteIndex) % 9 == 0 ? "DISLIKE" : "LIKE";
                upsertPodcastVote(user.profileId(), podcast.id(), voteType);
            }

            for (int voteIndex = 0; voteIndex < Math.min(10, playlistIds.size()); voteIndex++) {
                UUID playlistId = playlistIds.get((userIndex * 3 + voteIndex) % playlistIds.size());
                String voteType = (userIndex + voteIndex) % 11 == 0 ? "DISLIKE" : "LIKE";
                upsertPlaylistVote(user.profileId(), playlistId, voteType);
            }

            for (int historyIndex = 0; historyIndex < Math.min(24, published.size()); historyIndex++) {
                PodcastSeed podcast = published.get((userIndex * 11 + historyIndex) % published.size());
                int duration = podcast.durationSeconds() == null ? 1200 : podcast.durationSeconds();
                int progress = Math.max(15, (duration * ((historyIndex % 10) + 1)) / 11);
                boolean completed = historyIndex % 6 == 0;
                upsertListenHistory(user.profileId(), podcast.id(), progress, completed, userIndex + historyIndex + 1);
            }
        }
    }

    private void refreshCounters() {
        jdbcTemplate.update("""
                update author_profiles a
                   set subscribers_count = coalesce(s.count, 0)
                  from (
                    select author_id, count(*) as count
                    from subscriptions
                    group by author_id
                  ) s
                 where a.id = s.author_id
                """);
        jdbcTemplate.update("""
                update author_profiles
                   set subscribers_count = 0
                 where id not in (select distinct author_id from subscriptions)
                """);
        jdbcTemplate.update("""
                update podcasts p
                   set likes_count = (
                         select count(*) from podcast_votes v
                         where v.podcast_id = p.id and v.vote_type = 'LIKE'
                       ),
                       dislikes_count = (
                         select count(*) from podcast_votes v
                         where v.podcast_id = p.id and v.vote_type = 'DISLIKE'
                       )
                """);
        jdbcTemplate.update("""
                update playlists p
                   set likes_count = (
                         select count(*) from playlist_votes v
                         where v.playlist_id = p.id and v.vote_type = 'LIKE'
                       ),
                       dislikes_count = (
                         select count(*) from playlist_votes v
                         where v.playlist_id = p.id and v.vote_type = 'DISLIKE'
                       )
                """);
    }

    private void upsertUserProfile(UUID id, UUID userId, String username, String avatarUrl) {
        jdbcTemplate.update("""
                insert into user_profiles (id, user_id, username, avatar_url, theme, language, created_at)
                values (?, ?, ?, ?, ?, ?, now() - (? * interval '1 day'))
                on conflict (id) do update
                   set user_id = excluded.user_id,
                       username = excluded.username,
                       avatar_url = excluded.avatar_url,
                       theme = excluded.theme,
                       language = excluded.language
                """, id, userId, username, avatarUrl, username.hashCode() % 2 == 0 ? "DARK" : "LIGHT",
                username.contains("unicode") ? "EN" : "RU", Math.abs(username.hashCode() % 90));
    }

    private UUID existingUuid(String sql, Object... args) {
        List<UUID> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getObject(1, UUID.class),
                args
        );
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private void upsertAuthor(UUID id, UUID profileId, String authorName, String description) {
        jdbcTemplate.update("""
                insert into author_profiles (id, user_profile_id, author_name, description, subscribers_count, created_at)
                values (?, ?, ?, ?, 0, now() - (? * interval '1 day'))
                on conflict (id) do update
                   set user_profile_id = excluded.user_profile_id,
                       author_name = excluded.author_name,
                       description = excluded.description
                """, id, profileId, authorName, description, Math.abs(authorName.hashCode() % 180));
    }

    private void upsertCategory(UUID id, String name, int position) {
        jdbcTemplate.update("""
                insert into categories (id, name, position, created_at)
                values (?, ?, ?, now() - (? * interval '1 day'))
                on conflict (id) do update
                   set name = excluded.name,
                       position = excluded.position
                """, id, name, position, position / 10);
    }

    private void upsertPodcast(PodcastSeed podcast) {
        jdbcTemplate.update("""
                insert into podcasts (
                    id, author_id, category_id, title, description, cover_image_url, audio_url,
                    audio_url_file, audio_size_file, num_speakers,
                    duration_seconds, status, views_count, likes_count, dislikes_count, published_at, created_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        case when ? in ('PUBLISHED', 'ARCHIVED') then now() - (? * interval '1 day') else null end,
                        now() - (? * interval '1 day'))
                on conflict (id) do update
                   set author_id = excluded.author_id,
                       category_id = excluded.category_id,
                       title = excluded.title,
                       description = excluded.description,
                       cover_image_url = excluded.cover_image_url,
                       audio_url = excluded.audio_url,
                       audio_url_file = excluded.audio_url_file,
                       audio_size_file = excluded.audio_size_file,
                       num_speakers = excluded.num_speakers,
                       duration_seconds = excluded.duration_seconds,
                       status = excluded.status,
                       views_count = excluded.views_count,
                       likes_count = excluded.likes_count,
                       dislikes_count = excluded.dislikes_count,
                       published_at = excluded.published_at
                """, podcast.id(), podcast.authorId(), podcast.categoryId(), podcast.title(), podcast.description(),
                podcast.coverImageUrl(), podcast.audioUrl(), podcast.audioUrlFile(), podcast.audioSizeFile(),
                podcast.numSpeakers(), podcast.durationSeconds(), podcast.status(),
                podcast.viewsCount(), podcast.likesCount(), podcast.dislikesCount(), podcast.status(),
                podcast.daysAgo(), podcast.daysAgo() + 3);
    }

    private void upsertTranscript(UUID podcastId, String language, String content) {
        jdbcTemplate.update("""
                insert into podcast_transcripts (podcast_id, language, content, generated_at)
                values (?, ?, ?, now() - interval '1 day')
                on conflict (podcast_id, language) do update
                   set content = excluded.content,
                       generated_at = excluded.generated_at
                """, podcastId, language, content);
    }

    private void upsertSummary(UUID podcastId, String language, String content) {
        jdbcTemplate.update("""
                insert into podcast_summaries (podcast_id, language, content, generated_at)
                values (?, ?, ?, now() - interval '1 day')
                on conflict (podcast_id, language) do update
                   set content = excluded.content,
                       generated_at = excluded.generated_at
                """, podcastId, language, content);
    }

    private void upsertPlaylist(
            UUID id,
            UUID ownerProfileId,
            String title,
            String description,
            String coverImageUrl,
            boolean isPublic,
            long likes,
            long dislikes
    ) {
        jdbcTemplate.update("""
                insert into playlists (
                    id, owner_profile_id, title, description, cover_image_url, is_public,
                    likes_count, dislikes_count, created_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, now() - (? * interval '1 day'))
                on conflict (id) do update
                   set owner_profile_id = excluded.owner_profile_id,
                       title = excluded.title,
                       description = excluded.description,
                       cover_image_url = excluded.cover_image_url,
                       is_public = excluded.is_public,
                       likes_count = excluded.likes_count,
                       dislikes_count = excluded.dislikes_count
                """, id, ownerProfileId, title, description, coverImageUrl, isPublic, likes, dislikes,
                Math.abs(title.hashCode() % 120));
    }

    private void clearPlaylistItems(UUID playlistId) {
        jdbcTemplate.update("delete from playlist_podcasts where playlist_id = ?", playlistId);
    }

    private void upsertPlaylistPodcast(UUID playlistId, UUID podcastId, int position) {
        jdbcTemplate.update("""
                insert into playlist_podcasts (playlist_id, podcast_id, position, added_at)
                values (?, ?, ?, now() - (? * interval '1 hour'))
                on conflict (playlist_id, podcast_id) do update
                   set position = excluded.position,
                       added_at = excluded.added_at
                """, playlistId, podcastId, position, position * 3);
    }

    private void upsertSubscription(UUID subscriberProfileId, UUID authorId) {
        jdbcTemplate.update("""
                insert into subscriptions (subscriber_profile_id, author_id, subscribed_at)
                values (?, ?, now() - (? * interval '1 day'))
                on conflict (subscriber_profile_id, author_id) do update
                   set subscribed_at = excluded.subscribed_at
                """, subscriberProfileId, authorId, Math.abs((subscriberProfileId.hashCode() + authorId.hashCode()) % 180));
    }

    private void upsertPodcastVote(UUID userProfileId, UUID podcastId, String voteType) {
        jdbcTemplate.update("""
                insert into podcast_votes (user_profile_id, podcast_id, vote_type, created_at)
                values (?, ?, ?, now() - (? * interval '1 hour'))
                on conflict (user_profile_id, podcast_id) do update
                   set vote_type = excluded.vote_type,
                       created_at = excluded.created_at
                """, userProfileId, podcastId, voteType, Math.abs((userProfileId.hashCode() + podcastId.hashCode()) % 240));
    }

    private void upsertPlaylistVote(UUID userProfileId, UUID playlistId, String voteType) {
        jdbcTemplate.update("""
                insert into playlist_votes (user_profile_id, playlist_id, vote_type, created_at)
                values (?, ?, ?, now() - (? * interval '1 hour'))
                on conflict (user_profile_id, playlist_id) do update
                   set vote_type = excluded.vote_type,
                       created_at = excluded.created_at
                """, userProfileId, playlistId, voteType, Math.abs((userProfileId.hashCode() + playlistId.hashCode()) % 240));
    }

    private void upsertListenHistory(UUID userProfileId, UUID podcastId, int progressSeconds, boolean completed, int daysAgo) {
        jdbcTemplate.update("""
                insert into listen_history (user_profile_id, podcast_id, progress_seconds, completed)
                values (?, ?, ?, ?)
                on conflict (user_profile_id, podcast_id) do update
                   set progress_seconds = excluded.progress_seconds,
                       completed = excluded.completed
                """, userProfileId, podcastId, progressSeconds, completed);
        jdbcTemplate.update("""
                update listen_history
                   set last_listened_at = now() - (? * interval '1 day')
                 where user_profile_id = ? and podcast_id = ?
                """, daysAgo, userProfileId, podcastId);
    }

    private static String statusFor(int authorIndex, int episode) {
        if (episode <= 6) {
            return "PUBLISHED";
        }
        if (episode == 7) {
            return authorIndex % 2 == 0 ? "DRAFT" : "ARCHIVED";
        }
        if (episode == 8) {
            return authorIndex % 3 == 0 ? "PROCESSING" : "PUBLISHED";
        }
        return authorIndex % 4 == 0 ? "FAILED" : "PUBLISHED";
    }

    private static String buildPodcastTitle(String authorName, String topic, int episode, String status) {
        String suffix = switch (status) {
            case "DRAFT" -> "черновик";
            case "PROCESSING" -> "processing";
            case "FAILED" -> "failed";
            case "ARCHIVED" -> "архив";
            default -> "episode";
        };
        return topic + " — " + authorName + " #" + episode + " (" + suffix + ")";
    }

    private static String buildPodcastDescription(String authorName, String topic, int episode, String status) {
        if (episode % 8 == 0) {
            return null;
        }
        return "Dev episode от " + authorName + " про " + topic
                + ". Статус: " + status
                + ". Описание содержит русский текст, English fragments, цифры " + episode
                + " и достаточно длинный абзац для проверки переносов, карточек, поиска и detail-page.";
    }

    private static UUID uuid(String namespace, int index) {
        return uuid(namespace + ":" + index);
    }

    private static UUID uuid(String value) {
        return UUID.nameUUIDFromBytes(("podcast-core-dev:" + value).getBytes(StandardCharsets.UTF_8));
    }

    private record UserSeed(UUID profileId, UUID userId, String username, String avatarUrl) {
    }

    private record AuthorSeed(UUID id, UUID profileId, String name, String description) {
    }

    private record PodcastSeed(
            UUID id,
            UUID authorId,
            UUID categoryId,
            String title,
            String description,
            String coverImageUrl,
            String audioUrl,
            String audioUrlFile,
            Long audioSizeFile,
            int numSpeakers,
            Integer durationSeconds,
            String status,
            long viewsCount,
            long likesCount,
            long dislikesCount,
            int daysAgo
    ) {
    }
}
