package podcastService.podcast.controller;

import org.junit.jupiter.api.Test;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.podcast.service.PodcastService;
import podcastService.podcast.service.PodcastVoteService;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.service.PodcastMediaService;
import podcastService.transcript.summary.SummaryGenerationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PodcastControllerSummaryGenerationTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void manualGenerateSummaryEndpointReturnsServiceResponseWithoutForce() {
        SummaryGenerationService summaryGenerationService = mock(SummaryGenerationService.class);
        PodcastSummaryResponse expected = response("Existing summary");
        when(summaryGenerationService.generateForAuthor(PODCAST_ID, USER_ID, false)).thenReturn(expected);
        PodcastController controller = controller(summaryGenerationService);

        PodcastSummaryResponse actual = controller.generatePodcastSummary(PODCAST_ID, currentUser(), false);

        assertThat(actual).isEqualTo(expected);
        verify(summaryGenerationService).generateForAuthor(PODCAST_ID, USER_ID, false);
    }

    @Test
    void manualGenerateSummaryEndpointPassesForceTrue() {
        SummaryGenerationService summaryGenerationService = mock(SummaryGenerationService.class);
        PodcastSummaryResponse expected = response("Regenerated summary");
        when(summaryGenerationService.generateForAuthor(PODCAST_ID, USER_ID, true)).thenReturn(expected);
        PodcastController controller = controller(summaryGenerationService);

        PodcastSummaryResponse actual = controller.generatePodcastSummary(PODCAST_ID, currentUser(), true);

        assertThat(actual).isEqualTo(expected);
        verify(summaryGenerationService).generateForAuthor(PODCAST_ID, USER_ID, true);
    }

    private PodcastController controller(SummaryGenerationService summaryGenerationService) {
        return new PodcastController(
                mock(PodcastService.class),
                mock(PodcastVoteService.class),
                mock(PodcastMediaService.class),
                summaryGenerationService
        );
    }

    private PodcastSummaryResponse response(String content) {
        return new PodcastSummaryResponse(
                PODCAST_ID,
                "RU",
                content,
                OffsetDateTime.parse("2026-06-01T00:00:00Z")
        );
    }

    private AuthenticatedUser currentUser() {
        return new AuthenticatedUser(USER_ID, "author@example.test", List.of("AUTHOR"));
    }
}
