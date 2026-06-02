package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.transcript.service.VttSpeakerBlockParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranscriptTextResolverTest {

    private final SubtitleObjectClient subtitleObjectClient = mock(SubtitleObjectClient.class);
    private final TranscriptTextResolver resolver = new TranscriptTextResolver(
            new JacksonConfig().objectMapper(),
            subtitleObjectClient,
            new VttSpeakerBlockParser()
    );

    @Test
    void plainTranscriptIsReturnedAsText() {
        assertThat(resolver.resolve("  Это обычный transcript.  "))
                .isEqualTo("Это обычный transcript.");
    }

    @Test
    void ttsJsonArrayIsExtractedToText() {
        String result = resolver.resolve("""
                [
                  {"text":"Первая реплика","voice":"aidar"},
                  {"text":"Вторая реплика","voice":"kseniya"}
                ]
                """);

        assertThat(result)
                .contains("Первая реплика")
                .contains("Вторая реплика")
                .doesNotContain("aidar")
                .doesNotContain("kseniya");
    }

    @Test
    void subtitlePointerJsonFetchesVttAndBuildsConsecutiveSpeakerBlocks() {
        when(subtitleObjectClient.fetch("media/uuid/subtitles.vtt"))
                .thenReturn("""
                        WEBVTT

                        00:00:00.000 --> 00:00:02.000
                        SPEAKER_00: Первая часть.

                        00:00:02.000 --> 00:00:04.000
                        SPEAKER_00: Продолжение.

                        00:00:04.000 --> 00:00:06.000
                        SPEAKER_01: Ответ.

                        00:00:06.000 --> 00:00:08.000
                        SPEAKER_00: Новый блок первого спикера.
                        """);

        String result = resolver.resolve("""
                {
                  "vtt_object_key": "media/uuid/subtitles.vtt",
                  "srt_object_key": "media/uuid/subtitles.srt",
                  "ready_at": "2026-06-01T00:00:00Z"
                }
                """);

        assertThat(result).isEqualTo("""
                [{"text":"Первая часть. Продолжение.","voice":"speaker_00"},{"text":"Ответ.","voice":"speaker_01"},{"text":"Новый блок первого спикера.","voice":"speaker_00"}]\
                """);
        verify(subtitleObjectClient).fetch("media/uuid/subtitles.vtt");
    }

    @Test
    void subtitlePointerJsonFallsBackToSrtObjectAndCleansTimings() {
        when(subtitleObjectClient.fetch("media/uuid/subtitles.srt"))
                .thenReturn("""
                        1
                        00:00:00,000 --> 00:00:02,000
                        Первый фрагмент текста.

                        2
                        00:00:02,000 --> 00:00:04,000
                        Второй фрагмент текста.
                        """);

        String result = resolver.resolve("""
                {
                  "srt_object_key": "media/uuid/subtitles.srt",
                  "ready_at": "2026-06-01T00:00:00Z"
                }
                """);

        assertThat(result)
                .contains("Первый фрагмент текста.")
                .contains("Второй фрагмент текста.")
                .doesNotContain("-->")
                .doesNotContain("00:00");
        verify(subtitleObjectClient).fetch("media/uuid/subtitles.srt");
    }

    @Test
    void subtitlePointerJsonFallsBackToVttObject() {
        when(subtitleObjectClient.fetch("media/uuid/subtitles.vtt"))
                .thenReturn("""
                        WEBVTT

                        00:00:00.000 --> 00:00:02.000
                        Текст из webvtt.
                        """);

        String result = resolver.resolve("""
                {"vtt_object_key":"media/uuid/subtitles.vtt"}
                """);

        assertThat(result).isEqualTo("Текст из webvtt.");
        verify(subtitleObjectClient).fetch("media/uuid/subtitles.vtt");
    }
}
