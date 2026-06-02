package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PodcastTranscriptSavedListenerTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void listenerRunsGenerationForSavedTranscriptEvent() {
        SummaryGenerationService service = mock(SummaryGenerationService.class);
        PodcastTranscriptSavedListener listener = new PodcastTranscriptSavedListener(service);

        listener.onPodcastTranscriptSaved(new PodcastTranscriptSavedEvent(PODCAST_ID, "RU", "media.subtitle"));

        verify(service).generateIfMissing(PODCAST_ID, "RU");
    }

    @Test
    void listenerCatchesGenerationErrors() {
        SummaryGenerationService service = mock(SummaryGenerationService.class);
        doThrow(new OpenRouterClientException("OpenRouter unavailable"))
                .when(service).generateIfMissing(PODCAST_ID, "RU");
        PodcastTranscriptSavedListener listener = new PodcastTranscriptSavedListener(service);

        assertThatNoException()
                .isThrownBy(() -> listener.onPodcastTranscriptSaved(
                        new PodcastTranscriptSavedEvent(PODCAST_ID, "RU", "media.subtitle")
                ));
    }
}
