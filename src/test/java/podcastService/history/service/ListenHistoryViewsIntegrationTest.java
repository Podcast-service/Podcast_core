package podcastService.history.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.category.entity.CategoryEntity;
import podcastService.category.repository.CategoryRepository;
import podcastService.history.dto.SaveProgressRequest;
import podcastService.history.repository.ListenHistoryRepository;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "app.outbox.publisher.enabled=false",
        "app.kafka.producer.enabled=false",
        "app.recommendation.events.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class ListenHistoryViewsIntegrationTest {

    private static final UUID LISTENER_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final UUID LISTENER_PROFILE_ID = UUID.fromString("10000000-0000-0000-0000-000000000102");
    private static final UUID AUTHOR_USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000201");
    private static final UUID AUTHOR_PROFILE_ID = UUID.fromString("10000000-0000-0000-0000-000000000202");
    private static final UUID AUTHOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000203");
    private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000301");
    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000401");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("podcast_db")
            .withUsername("podcast_user")
            .withPassword("podcast_pass");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ListenHistoryService listenHistoryService;

    @Autowired
    private ListenHistoryRepository listenHistoryRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        listenHistoryRepository.deleteAll();
        podcastRepository.deleteAll();
        authorRepository.deleteAll();
        categoryRepository.deleteAll();
        userProfileRepository.deleteAll();

        savePodcastFixture();
        entityManager.clear();
    }

    @Test
    void firstPositiveProgressCountsViewOnce() {
        listenHistoryService.saveProgress(PODCAST_ID, LISTENER_USER_ID, new SaveProgressRequest(12));

        assertThat(viewsCount()).isEqualTo(1L);
        assertThat(viewCounted()).isTrue();
    }

    @Test
    void repeatedPositiveProgressDoesNotCountViewAgain() {
        listenHistoryService.saveProgress(PODCAST_ID, LISTENER_USER_ID, new SaveProgressRequest(12));
        listenHistoryService.saveProgress(PODCAST_ID, LISTENER_USER_ID, new SaveProgressRequest(20));

        assertThat(viewsCount()).isEqualTo(1L);
        assertThat(progressSeconds()).isEqualTo(20);
        assertThat(viewCounted()).isTrue();
    }

    @Test
    void zeroProgressThenPositiveProgressCountsViewOnceOnPositiveTransition() {
        listenHistoryService.saveProgress(PODCAST_ID, LISTENER_USER_ID, new SaveProgressRequest(0));

        assertThat(viewsCount()).isZero();
        assertThat(viewCounted()).isFalse();

        listenHistoryService.saveProgress(PODCAST_ID, LISTENER_USER_ID, new SaveProgressRequest(15));

        assertThat(viewsCount()).isEqualTo(1L);
        assertThat(viewCounted()).isTrue();
    }

    @Test
    void parallelPositiveProgressCountsViewOnlyOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> saveProgressAfter(start));
            Future<?> second = executor.submit(() -> saveProgressAfter(start));

            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(viewsCount()).isEqualTo(1L);
        assertThat(viewCounted()).isTrue();
    }

    private void saveProgressAfter(CountDownLatch start) {
        try {
            start.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
        listenHistoryService.saveProgress(PODCAST_ID, LISTENER_USER_ID, new SaveProgressRequest(12));
    }

    private long viewsCount() {
        return jdbcTemplate.queryForObject(
                "select views_count from podcasts where id = ?",
                Long.class,
                PODCAST_ID
        );
    }

    private int progressSeconds() {
        return jdbcTemplate.queryForObject(
                "select progress_seconds from listen_history where user_profile_id = ? and podcast_id = ?",
                Integer.class,
                LISTENER_PROFILE_ID,
                PODCAST_ID
        );
    }

    private boolean viewCounted() {
        return jdbcTemplate.queryForObject(
                "select view_counted from listen_history where user_profile_id = ? and podcast_id = ?",
                Boolean.class,
                LISTENER_PROFILE_ID,
                PODCAST_ID
        );
    }

    private void savePodcastFixture() {
        UserProfileEntity listener = new UserProfileEntity();
        listener.setId(LISTENER_PROFILE_ID);
        listener.setUserId(LISTENER_USER_ID);
        listener.setUsername("listener-views");
        userProfileRepository.save(listener);

        UserProfileEntity authorOwner = new UserProfileEntity();
        authorOwner.setId(AUTHOR_PROFILE_ID);
        authorOwner.setUserId(AUTHOR_USER_ID);
        authorOwner.setUsername("author-views");
        userProfileRepository.save(authorOwner);

        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        author.setUserProfile(authorOwner);
        author.setAuthorName("Author Views");
        authorRepository.save(author);

        CategoryEntity category = new CategoryEntity();
        category.setId(CATEGORY_ID);
        category.setName("Views");
        category.setPosition(10);
        categoryRepository.save(category);

        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setAuthor(author);
        podcast.setCategory(category);
        podcast.setTitle("View Count Podcast");
        podcast.setDescription("Podcast for view count integration tests");
        podcast.setAudioUrl("https://cdn.example.local/audio/view-count/master.m3u8");
        podcast.setDurationSeconds(100L);
        podcast.setNumSpeakers(1);
        podcast.setStatus(Status.PUBLISHED);
        podcast.setViewsCount(0L);
        podcast.setPublishedAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
        podcastRepository.saveAndFlush(podcast);
    }
}
