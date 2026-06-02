package podcastService.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminPodcastFilter;
import podcastService.admin.dto.AdminPodcastResponse;
import podcastService.admin.mapper.AdminMapper;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.entity.CategoryEntity;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;
import podcastService.user.entity.UserProfileEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPodcastServiceTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PODCAST_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

    private final PodcastRepository podcastRepository = mock(PodcastRepository.class);
    private final PodcastTranscriptRepository podcastTranscriptRepository = mock(PodcastTranscriptRepository.class);
    private final PodcastSummaryRepository podcastSummaryRepository = mock(PodcastSummaryRepository.class);
    private final RecommendationOutboxEventService recommendationOutboxEventService =
            mock(RecommendationOutboxEventService.class);
    private final AdminPodcastService service = new AdminPodcastService(
            podcastRepository,
            podcastTranscriptRepository,
            podcastSummaryRepository,
            recommendationOutboxEventService,
            new AdminMapper()
    );

    @SuppressWarnings("unchecked")
    @Test
    void getPodcastsReturnsArchivedPodcastsForAdmin() {
        PodcastEntity podcast = podcast(Status.ARCHIVED);
        when(podcastRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(podcast)));
        when(podcastTranscriptRepository.findPodcastIdsWithContent(Set.of(PODCAST_ID))).thenReturn(Set.of(PODCAST_ID));
        when(podcastSummaryRepository.findPodcastIdsWithSummary(Set.of(PODCAST_ID))).thenReturn(Set.of());

        AdminPageResponse<AdminPodcastResponse> response = service.getPodcasts(new AdminPodcastFilter(
                null,
                null,
                null,
                null,
                0,
                20,
                SortPodcasts.DATE_DESC
        ));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(PODCAST_ID);
        assertThat(response.items().getFirst().status()).isEqualTo("ARCHIVED");
        assertThat(response.items().getFirst().hasTranscript()).isTrue();
        assertThat(response.items().getFirst().hasSummary()).isFalse();
    }

    @Test
    void deletePodcastArchivesAnyPodcastWithoutOwnerCheck() {
        PodcastEntity podcast = podcast(Status.PUBLISHED);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastRepository.saveAndFlush(podcast)).thenReturn(podcast);

        service.deletePodcast(PODCAST_ID, ADMIN_USER_ID);

        assertThat(podcast.getStatus()).isEqualTo(Status.ARCHIVED);
        verify(podcastRepository).saveAndFlush(podcast);
        verify(recommendationOutboxEventService).savePodcastContentEvent(any(), any());
    }

    private PodcastEntity podcast(Status status) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setId(PROFILE_ID);
        profile.setUserId(UUID.randomUUID());
        profile.setUsername("author-user");

        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        author.setUserProfile(profile);
        author.setAuthorName("Admin Test Author");

        CategoryEntity category = new CategoryEntity();
        category.setId(CATEGORY_ID);
        category.setName("Technology");
        category.setPosition(10);

        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setAuthor(author);
        podcast.setCategory(category);
        podcast.setTitle("Kafka in production");
        podcast.setDescription("Episode description");
        podcast.setDurationSeconds(2580L);
        podcast.setNumSpeakers(2);
        podcast.setStatus(status);
        return podcast;
    }
}
