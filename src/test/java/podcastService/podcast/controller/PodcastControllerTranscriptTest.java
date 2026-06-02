package podcastService.podcast.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import podcastService.podcast.service.PodcastService;
import podcastService.podcast.service.PodcastVoteService;
import podcastService.transcript.dto.PodcastTranscriptResponse;
import podcastService.transcript.service.PodcastMediaService;
import podcastService.transcript.summary.SummaryGenerationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PodcastControllerTranscriptTest {

    private static final UUID PODCAST_ID = UUID.fromString("6a8b9f15-0456-48e6-93e9-0c0568126974");

    @Test
    void transcriptEndpointReturnsSpeakerBlocksAsJsonArray() throws Exception {
        PodcastMediaService podcastMediaService = mock(PodcastMediaService.class);
        when(podcastMediaService.getTranscript(PODCAST_ID)).thenReturn(new PodcastTranscriptResponse(
                PODCAST_ID,
                "RU",
                List.of(Map.of("text", "О чём идёт речь?", "voice", "speaker_00")),
                OffsetDateTime.parse("2026-06-02T03:26:32.678533Z")
        ));
        PodcastController controller = new PodcastController(
                mock(PodcastService.class),
                mock(PodcastVoteService.class),
                podcastMediaService,
                mock(SummaryGenerationService.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/podcasts/{podcastId}/transcript", PODCAST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].text").value("О чём идёт речь?"))
                .andExpect(jsonPath("$.content[0].voice").value("speaker_00"))
                .andExpect(jsonPath("$.content.array").doesNotExist());
    }
}
