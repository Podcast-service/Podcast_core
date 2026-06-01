package podcastService.podcast.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.infrastructure.messaging.error.KafkaRetryableProcessingException;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.entity.PodcastTranscriptId;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastMediaMetadataService {

    private static final String DEFAULT_LANGUAGE = "RU";
    private static final int MAX_MEDIA_PATH_LENGTH = 4096;
    private static final int MAX_TRANSCRIPT_CONTENT_LENGTH = 500_000;

    private final PodcastRepository podcastRepository;
    private final PodcastTranscriptRepository podcastTranscriptRepository;
    private final ObjectMapper objectMapper;
    private final PodcastMediaStatusTransitionPolicy transitionPolicy;

    @Transactional
    public void markFileUploadStarted(UUID podcastId, OffsetDateTime eventTimestamp) {
        PodcastEntity podcast = findForUpdate(podcastId);
        persistMediaState(podcast, Status.UPLOADING, eventTimestamp, "media upload started", ignored -> {
        });
    }

    @Transactional
    public void markFileUploaded(
            UUID podcastId,
            String audioUrlFile,
            OffsetDateTime eventTimestamp
    ) {
        PodcastEntity podcast = findForUpdate(podcastId);
        persistMediaState(podcast, Status.UPLOADED, eventTimestamp, "media uploaded", entity -> {
            entity.setAudioUrlFile(normalizeMediaPath(audioUrlFile, "audio_url_file"));
        });
    }

    @Transactional
    public void markProcessingStarted(UUID podcastId, OffsetDateTime eventTimestamp) {
        PodcastEntity podcast = findForUpdate(podcastId);
        persistMediaState(podcast, Status.PROCESSING, eventTimestamp, "media processing started", ignored -> {
        });
    }

    @Transactional
    public void markProcessed(
            UUID podcastId,
            String audioUrl,
            Long durationSeconds,
            Long audioFileSize,
            OffsetDateTime eventTimestamp
    ) {
        PodcastEntity podcast = findForUpdate(podcastId);
        persistMediaState(podcast, Status.PROCESSED, eventTimestamp, "media processed",
                entity -> {
                    entity.setAudioUrl(normalizeMediaPath(audioUrl, "audio_url"));
                    entity.setDurationSeconds(normalizeDurationSeconds(durationSeconds));
                    entity.setAudioSizeFile(normalizeAudioFileSize(audioFileSize));
                });
    }

    @Transactional
    public void markFailed(UUID podcastId, String errorMessage, OffsetDateTime eventTimestamp, String source) {
        PodcastEntity podcast = findForUpdate(podcastId);
        if (podcast.getStatus() == Status.PUBLISHED || podcast.getStatus() == Status.ARCHIVED) {
            log.warn(
                    "Media error ignored for terminal podcast, podcastId={}, currentStatus={}, source={}, error={}",
                    podcastId, podcast.getStatus(), source, safeError(errorMessage)
            );
            return;
        }
        podcast.setStatus(Status.FAILED);
        podcastRepository.saveAndFlush(podcast);
        log.warn("Podcast media failed, podcastId={}, status={}, source={}, eventTimestamp={}, error={}",
                podcastId, Status.FAILED, source, eventTimestamp, safeError(errorMessage));
    }

    @Transactional
    public void updateCoverFromMediaEvent(UUID podcastId, String coverImageUrl, OffsetDateTime eventTimestamp) {
        PodcastEntity podcast = findForUpdate(podcastId);
        podcast.setCoverImageUrl(normalizeMediaPath(coverImageUrl, "podcast cover"));
        podcastRepository.saveAndFlush(podcast);
        log.info("Podcast cover updated from Kafka media event, podcastId={}, eventTimestamp={}", podcastId, eventTimestamp);
    }

    @Transactional
    public void saveSubtitleContent(UUID podcastId, String vttObjectKey, String srtObjectKey, OffsetDateTime readyAt) {
        PodcastEntity podcast = findForUpdate(podcastId);
        PodcastTranscriptEntity transcript = findTranscriptOrNew(podcast);
        String vtt = normalizeMediaPath(vttObjectKey, "vtt_object_key");
        String srt = normalizeMediaPath(srtObjectKey, "srt_object_key");
        transcript.setContent(serializeSubtitleContent(vtt, srt, readyAt));
        podcastTranscriptRepository.saveAndFlush(transcript);
        log.info("Podcast subtitle content saved, podcastId={}, readyAt={}", podcastId, readyAt);
    }

    @Transactional
    public void saveTranscriptContent(UUID podcastId, JsonNode content, OffsetDateTime timestamp, String source) {
        PodcastEntity podcast = findForUpdate(podcastId);
        PodcastTranscriptEntity transcript = findTranscriptOrNew(podcast);
        transcript.setContent(normalizeTranscriptContent(serializeContentNode(content), source + " content"));
        podcastTranscriptRepository.saveAndFlush(transcript);
        log.info("Podcast transcript content saved from Kafka, podcastId={}, source={}, timestamp={}",
                podcastId, source, timestamp);
    }

    @Transactional
    public void saveTtsContent(UUID podcastId, String content, OffsetDateTime timestamp) {
        PodcastEntity podcast = findForUpdate(podcastId);
        PodcastTranscriptEntity transcript = findTranscriptOrNew(podcast);
        transcript.setContent(normalizeTranscriptContent(content, "tts content"));
        podcastTranscriptRepository.saveAndFlush(transcript);
        log.info("Podcast TTS content saved, podcastId={}, timestamp={}", podcastId, timestamp);
    }

    @Transactional
    public void saveTtsContentAndMarkUploading(UUID podcastId, String content, OffsetDateTime timestamp) {
        PodcastEntity podcast = findForUpdate(podcastId);
        PodcastTranscriptEntity transcript = findTranscriptOrNew(podcast);
        transcript.setContent(normalizeTranscriptContent(content, "tts content"));
        podcastTranscriptRepository.saveAndFlush(transcript);
        persistMediaState(podcast, Status.UPLOADING, timestamp, "tts started", ignored -> {
        });
        log.info("Podcast TTS content saved and upload lifecycle started, podcastId={}, timestamp={}",
                podcastId, timestamp);
    }

    @Transactional
    public void saveTtsContentAndMarkUploading(UUID podcastId, JsonNode content, OffsetDateTime timestamp) {
        PodcastEntity podcast = findForUpdate(podcastId);
        PodcastTranscriptEntity transcript = findTranscriptOrNew(podcast);
        transcript.setContent(normalizeTranscriptContent(serializeContentNode(content), "tts content"));
        podcastTranscriptRepository.saveAndFlush(transcript);
        persistMediaState(podcast, Status.UPLOADING, timestamp, "tts started", ignored -> {
        });
        log.info("Podcast TTS content saved and upload lifecycle started, podcastId={}, timestamp={}",
                podcastId, timestamp);
    }

    private void persistMediaState(
            PodcastEntity podcast,
            Status target,
            OffsetDateTime eventTimestamp,
            String reason,
            Consumer<PodcastEntity> mutation
    ) {
        UUID podcastId = podcast.getId();
        if (!transitionPolicy.canMoveTo(podcast.getStatus(), target)) {
            log.warn(
                    "Invalid media status transition ignored, podcastId={}, currentStatus={}, targetStatus={}, reason={}, eventTimestamp={}",
                    podcastId, podcast.getStatus(), target, reason, eventTimestamp
            );
            return;
        }

        Status previous = podcast.getStatus();
        mutation.accept(podcast);
        podcast.setStatus(target);
        podcastRepository.saveAndFlush(podcast);
        log.info("Podcast media status changed, podcastId={}, from={}, to={}, reason={}, eventTimestamp={}",
                podcastId, previous, target, reason, eventTimestamp);
    }

    private PodcastEntity findForUpdate(UUID podcastId) {
        if (podcastId == null) {
            throw new InvalidKafkaMessageException("Received null podcast id in Kafka event");
        }

        return podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new KafkaRetryableProcessingException(
                        "Podcast not found for Kafka media event, podcastId=" + podcastId,
                        null
                ));
    }

    private PodcastTranscriptEntity findTranscriptOrNew(PodcastEntity podcast) {
        return podcastTranscriptRepository
                .findByIdPodcastIdAndIdLanguage(podcast.getId(), DEFAULT_LANGUAGE)
                .orElseGet(() -> {
                    PodcastTranscriptEntity transcript = new PodcastTranscriptEntity();
                    transcript.setId(new PodcastTranscriptId(podcast.getId(), DEFAULT_LANGUAGE));
                    transcript.setPodcast(podcast);
                    return transcript;
                });
    }

    private String normalizeMediaPath(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidKafkaMessageException("Received blank " + fieldName + " in Kafka event");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_MEDIA_PATH_LENGTH) {
            throw new InvalidKafkaMessageException("Received too long " + fieldName + " in Kafka event");
        }
        return normalized;
    }

    private String normalizeTranscriptContent(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidKafkaMessageException("Received blank " + fieldName + " in Kafka event");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TRANSCRIPT_CONTENT_LENGTH) {
            throw new InvalidKafkaMessageException("Received too long " + fieldName + " in Kafka event");
        }
        return normalized;
    }

    private Long normalizeDurationSeconds(Long value) {
        if (value == null) {
            throw new InvalidKafkaMessageException("Received null duration_seconds in Kafka event");
        }
        if (value <= 0) {
            throw new InvalidKafkaMessageException("Received non-positive duration_seconds in Kafka event");
        }
        return value;
    }

    private Long normalizeAudioFileSize(Long value) {
        if (value == null) {
            throw new InvalidKafkaMessageException("Received null audio_file_size in Kafka event");
        }
        if (value < 0) {
            throw new InvalidKafkaMessageException("Received negative audio_file_size in Kafka event");
        }
        return value;
    }

    private String safeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }

    private String serializeSubtitleContent(String vttObjectKey, String srtObjectKey, OffsetDateTime readyAt) {
        try {
            return objectMapper.writeValueAsString(new SubtitleContent(vttObjectKey, srtObjectKey, readyAt));
        } catch (JsonProcessingException exception) {
            throw new InvalidKafkaMessageException("Failed to serialize subtitle content", exception);
        }
    }

    private String serializeContentNode(JsonNode content) {
        if (content == null || content.isNull()) {
            throw new InvalidKafkaMessageException("Received null content in Kafka event");
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isContainerNode()) {
            return content.toString();
        }
        return content.asText();
    }

    private record SubtitleContent(
            @JsonProperty("vtt_object_key")
            String vttObjectKey,
            @JsonProperty("srt_object_key")
            String srtObjectKey,
            @JsonProperty("ready_at")
            OffsetDateTime readyAt
    ) {
    }
}
