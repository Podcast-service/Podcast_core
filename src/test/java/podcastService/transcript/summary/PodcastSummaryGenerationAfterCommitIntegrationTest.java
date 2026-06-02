package podcastService.transcript.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.category.entity.CategoryEntity;
import podcastService.category.repository.CategoryRepository;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.service.PodcastMediaMetadataService;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "app.outbox.publisher.enabled=false",
        "app.kafka.producer.enabled=false",
        "app.recommendation.events.enabled=false",
        "app.openrouter.enabled=true"
})
@Testcontainers(disabledWithoutDocker = true)
class PodcastSummaryGenerationAfterCommitIntegrationTest {

    private static final UUID USER_PROFILE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID AUTHOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");

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

    @Autowired private PodcastMediaMetadataService podcastMediaMetadataService;
    @Autowired private PodcastSummaryRepository podcastSummaryRepository;
    @Autowired private PodcastTranscriptRepository podcastTranscriptRepository;
    @Autowired private PodcastRepository podcastRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OpenRouterClient openRouterClient;

    @BeforeEach
    void setUp() {
        podcastSummaryRepository.deleteAll();
        podcastTranscriptRepository.deleteAll();
        podcastRepository.deleteAll();
        authorRepository.deleteAll();
        categoryRepository.deleteAll();
        userProfileRepository.deleteAll();
        reset(openRouterClient);
    }

    @Test
    void transcriptSavedAfterCommitEventCanPersistSummaryInNewTransaction() {
        savePodcastFixture();
        when(openRouterClient.complete(anyList())).thenReturn("Generated summary after commit");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(ignored -> podcastMediaMetadataService.saveTranscriptContent(
                PODCAST_ID,
                objectMapper.valueToTree("Это достаточно длинный transcript подкаста для генерации summary после commit."),
                OffsetDateTime.parse("2026-06-01T00:00:00Z"),
                "media.subtitle"
        ));

        entityManager.clear();
        assertThat(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .isPresent()
                .get()
                .extracting(summary -> summary.getContent())
                .isEqualTo("Generated summary after commit");
    }

    private void savePodcastFixture() {
        UserProfileEntity userProfile = new UserProfileEntity();
        userProfile.setId(USER_PROFILE_ID);
        userProfile.setUserId(USER_ID);
        userProfile.setUsername("summary-author");
        userProfileRepository.saveAndFlush(userProfile);

        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        author.setUserProfile(userProfile);
        author.setAuthorName("Summary Author");
        authorRepository.saveAndFlush(author);

        CategoryEntity category = new CategoryEntity();
        category.setId(CATEGORY_ID);
        category.setName("Technology");
        category.setPosition(10);
        categoryRepository.saveAndFlush(category);

        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setAuthor(author);
        podcast.setCategory(category);
        podcast.setTitle("Summary pipeline");
        podcast.setStatus(Status.PUBLISHED);
        podcast.setNumSpeakers(2);
        podcast.setDurationSeconds(1200L);
        podcast.setAudioUrl("https://cdn.example.local/audio/summary-pipeline/master.m3u8");
        podcast.setPublishedAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
        podcastRepository.saveAndFlush(podcast);
    }

    @TestConfiguration
    static class TestOpenRouterConfiguration {
        @Bean
        @Primary
        OpenRouterClient testOpenRouterClient() {
            return Mockito.mock(OpenRouterClient.class);
        }
    }
}
