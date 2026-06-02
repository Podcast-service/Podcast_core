package podcastService.transcript.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.transcript.summary.SubtitleObjectClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubtitleTranscriptServiceTest {

    private static final String VTT_URL = "https://storage.example/media/podcast/subtitles.vtt";

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private final SubtitleObjectClient subtitleObjectClient = mock(SubtitleObjectClient.class);
    private final SubtitleTranscriptService service = new SubtitleTranscriptService(
            subtitleObjectClient,
            new VttSpeakerBlockParser(),
            objectMapper
    );

    @Test
    void subtitlePointerJsonIsConvertedToConsecutiveSpeakerBlocks() throws Exception {
        when(subtitleObjectClient.fetch(VTT_URL)).thenReturn("""
                WEBVTT

                00:00:00.000 --> 00:00:01.000
                SPEAKER_00: Первая часть.

                00:00:01.000 --> 00:00:02.000
                SPEAKER_00: Вторая часть.

                00:00:02.000 --> 00:00:03.000
                SPEAKER_01: Ответ.

                00:00:03.000 --> 00:00:04.000
                SPEAKER_00: Новый блок.
                """);

        JsonNode result = service.buildSpeakerBlocks(objectMapper.readTree("""
                {
                  "vtt_object_key": "https://storage.example/media/podcast/subtitles.vtt",
                  "srt_object_key": "https://storage.example/media/podcast/subtitles.srt"
                }
                """));

        assertThat(result).isEqualTo(objectMapper.readTree("""
                [
                  {"text":"Первая часть. Вторая часть.","voice":"speaker_00"},
                  {"text":"Ответ.","voice":"speaker_01"},
                  {"text":"Новый блок.","voice":"speaker_00"}
                ]
                """));
        verify(subtitleObjectClient).fetch(VTT_URL);
    }

    @Test
    void missingVttObjectKeyIsRejected() throws Exception {
        assertThatThrownBy(() -> service.buildSpeakerBlocks(objectMapper.readTree("""
                {"srt_object_key":"https://storage.example/media/podcast/subtitles.srt"}
                """)))
                .isInstanceOf(InvalidKafkaMessageException.class)
                .hasMessage("media.subtitle content has missing vtt_object_key");
    }

    @Test
    void subtitleWithoutSpeakerBlocksIsRejected() throws Exception {
        when(subtitleObjectClient.fetch(VTT_URL)).thenReturn("""
                WEBVTT

                00:00:00.000 --> 00:00:01.000
                Текст без спикера.
                """);

        assertThatThrownBy(() -> service.buildSpeakerBlocks(objectMapper.readTree("""
                {"vtt_object_key":"https://storage.example/media/podcast/subtitles.vtt"}
                """)))
                .isInstanceOf(InvalidKafkaMessageException.class)
                .hasMessage("Subtitle VTT does not contain speaker blocks");
    }
}
