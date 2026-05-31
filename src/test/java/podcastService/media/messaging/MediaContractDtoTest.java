package podcastService.media.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.messaging.error.KafkaDeserializationException;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaSubtitleEventDto;
import podcastService.media.messaging.contract.MediaUploadEventDto;
import podcastService.media.messaging.contract.MediaUploadEventType;
import podcastService.media.messaging.contract.MediaWorkerEventDto;
import podcastService.media.messaging.contract.MediaWorkerEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaContractDtoTest {

    private static final KafkaRecordContext CONTEXT = new KafkaRecordContext(
            "media.upload", 0, 1L, null, null, null
    );

    private final KafkaMessageReader reader = new KafkaMessageReader(new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void readsMediaUploadDtoAndMapsDuration() {
        MediaUploadEventDto event = reader.read("""
                {
                  "object_type": "podcast_file_url",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "event": "uploaded",
                  "audio_url_file": "https://storage.example.local/source.mp3",
                  "duration_seconds": 2400,
                  "timestamp": "2026-03-22T12:35:56Z",
                  "extra": "ignored"
                }
                """, MediaUploadEventDto.class, CONTEXT);

        assertThat(event.objectType()).isEqualTo(MediaObjectType.PODCAST_FILE_URL);
        assertThat(event.event()).isEqualTo(MediaUploadEventType.UPLOADED);
        assertThat(event.objectId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000301"));
        assertThat(event.durationSeconds()).isEqualTo(2400L);
        assertThat(event.timestamp()).isEqualTo(OffsetDateTime.parse("2026-03-22T12:35:56Z"));
    }

    @Test
    void readsUploadAliasesFromAdjacentMediaService() {
        MediaUploadEventDto event = reader.read("""
                {
                  "object_type": "podcast_file",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "event": "upload_completed",
                  "audio_file_url": "https://storage.example.local/source.mp3",
                  "durationSeconds": 2400,
                  "timestamp": "2026-03-22T12:35:56Z"
                }
                """, MediaUploadEventDto.class, CONTEXT);

        assertThat(event.normalizedObjectType()).isEqualTo(MediaObjectType.PODCAST_FILE_URL);
        assertThat(event.normalizedEvent()).isEqualTo(MediaUploadEventType.UPLOADED);
        assertThat(event.audioUrlFile()).isEqualTo("https://storage.example.local/source.mp3");
        assertThat(event.durationSeconds()).isEqualTo(2400L);
    }

    @Test
    void unsupportedUploadEventIsRejectedAsDeserializationError() {
        assertThatThrownBy(() -> reader.read("""
                {
                  "object_type": "podcast_file_url",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "event": "not_our_event",
                  "timestamp": "2026-03-22T12:35:56Z"
                }
                """, MediaUploadEventDto.class, CONTEXT))
                .isInstanceOf(KafkaDeserializationException.class)
                .hasMessageContaining("Failed to deserialize Kafka payload");
    }

    @Test
    void imageUploadDefaultsMissingEventToUploadedAndUsesImageUrl() {
        MediaUploadEventDto event = reader.read("""
                {
                  "object_type": "podcast_cover_url",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "image_url": "https://storage.example.local/covers/podcast.jpg",
                  "timestamp": "2026-03-22T12:35:56Z"
                }
                """, MediaUploadEventDto.class, CONTEXT);

        assertThat(event.normalizedObjectType()).isEqualTo(MediaObjectType.PODCAST_COVER_URL);
        assertThat(event.normalizedEvent()).isEqualTo(MediaUploadEventType.UPLOADED);
        assertThat(event.uploadedImageUrl()).isEqualTo("https://storage.example.local/covers/podcast.jpg");
    }

    @Test
    void normalizesUploadErrorWithoutObjectTypeAndEvent() {
        MediaUploadEventDto event = reader.read("""
                {
                  "podcast_id": "00000000-0000-0000-0000-000000000301",
                  "error": "upload failed",
                  "timestamp": "2026-03-22T12:35:56Z"
                }
                """, MediaUploadEventDto.class, CONTEXT);

        assertThat(event.normalizedObjectType()).isEqualTo(MediaObjectType.PODCAST_FILE_URL);
        assertThat(event.normalizedEvent()).isEqualTo(MediaUploadEventType.UPLOAD_FAILED);
        assertThat(event.targetPodcastId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000301"));
    }

    @Test
    void normalizesWorkerErrorWithoutObjectTypeAndEvent() {
        MediaWorkerEventDto event = reader.read("""
                {
                  "podcast_id": "00000000-0000-0000-0000-000000000301",
                  "error": "processing failed",
                  "timestamp": "2026-03-22T12:35:56Z"
                }
                """, MediaWorkerEventDto.class, CONTEXT);

        assertThat(event.normalizedObjectType()).isEqualTo(MediaObjectType.PODCAST_FILE_URL);
        assertThat(event.normalizedEvent()).isEqualTo(MediaWorkerEventType.PROCESSING_FAILED);
    }

    @Test
    void readsWorkerAliasesFromAdjacentMediaService() {
        MediaWorkerEventDto event = reader.read("""
                {
                  "object_type": "podcast_file",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "event": "processing_completed",
                  "hls_url": "https://cdn.example.local/hls/master.m3u8",
                  "timestamp": "2026-03-22T12:35:56Z"
                }
                """, MediaWorkerEventDto.class, CONTEXT);

        assertThat(event.normalizedObjectType()).isEqualTo(MediaObjectType.PODCAST_FILE_URL);
        assertThat(event.normalizedEvent()).isEqualTo(MediaWorkerEventType.PROCESSED);
        assertThat(event.audioUrl()).isEqualTo("https://cdn.example.local/hls/master.m3u8");
    }

    @Test
    void readsSubtitleContentAsTextOrObject() {
        MediaSubtitleEventDto textEvent = reader.read("""
                {
                  "podcast_id": "00000000-0000-0000-0000-000000000301",
                  "content": "WEBVTT\\n\\n00:00.000 --> 00:01.000\\nПривет",
                  "ready_at": "2026-03-22T12:35:56Z"
                }
                """, MediaSubtitleEventDto.class, CONTEXT);

        MediaSubtitleEventDto objectEvent = reader.read("""
                {
                  "podcast_id": "00000000-0000-0000-0000-000000000301",
                  "content": {
                    "vtt_object_key": "media/podcast/subtitles.vtt",
                    "srt_object_key": "media/podcast/subtitles.srt"
                  },
                  "ready_at": "2026-03-22T12:35:56Z"
                }
                """, MediaSubtitleEventDto.class, CONTEXT);

        assertThat(textEvent.content().isTextual()).isTrue();
        assertThat(objectEvent.content().isObject()).isTrue();
        assertThat(objectEvent.content().get("vtt_object_key").asText())
                .isEqualTo("media/podcast/subtitles.vtt");
    }
}
