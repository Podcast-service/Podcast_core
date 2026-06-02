package podcastService.media.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaSubtitleEventDto;
import podcastService.podcast.service.PodcastMediaMetadataService;
import podcastService.transcript.service.SubtitleTranscriptService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaSubtitleConsumerTest {

    private static final UUID PODCAST_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final OffsetDateTime READY_AT = OffsetDateTime.parse("2026-03-22T12:35:56Z");

    private final KafkaMessageReader messageReader = mock(KafkaMessageReader.class);
    private final PodcastMediaMetadataService metadataService = mock(PodcastMediaMetadataService.class);
    private final SubtitleTranscriptService subtitleTranscriptService = mock(SubtitleTranscriptService.class);
    private final MediaSubtitleConsumer consumer = new MediaSubtitleConsumer(
            messageReader,
            metadataService,
            subtitleTranscriptService
    );

    @Test
    void transformedSpeakerBlocksArePersistedInsteadOfSubtitlePointers() throws Exception {
        JsonNode pointerContent = new JacksonConfig().objectMapper().readTree("""
                {"vtt_object_key":"https://storage.example/media/podcast/subtitles.vtt"}
                """);
        JsonNode speakerBlocks = new JacksonConfig().objectMapper().readTree("""
                [{"text":"Первая часть. Вторая часть.","voice":"speaker_00"}]
                """);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("media.subtitle", 0, 1L, null, "{}");
        when(messageReader.read(eq("{}"), eq(MediaSubtitleEventDto.class), any(KafkaRecordContext.class)))
                .thenReturn(new MediaSubtitleEventDto(PODCAST_ID, pointerContent, READY_AT));
        when(subtitleTranscriptService.buildSpeakerBlocks(pointerContent)).thenReturn(speakerBlocks);

        consumer.onMessage(record);

        verify(metadataService).saveTranscriptContent(PODCAST_ID, speakerBlocks, READY_AT, "media.subtitle");
    }
}
