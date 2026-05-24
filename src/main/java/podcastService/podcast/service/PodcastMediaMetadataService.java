package podcastService.podcast.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.infrastructure.messaging.error.KafkaRetryableProcessingException;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastMediaMetadataService {

    private static final Set<Status> FILE_START_TERMINAL_STATUSES = Set.of(
            Status.READY_TO_PUBLISH,
            Status.PUBLISHED,
            Status.ARCHIVED
    );

    private static final Set<Status> FILE_ERROR_TERMINAL_STATUSES = Set.of(
            Status.READY_TO_PUBLISH,
            Status.PUBLISHED,
            Status.ARCHIVED
    );

    private final PodcastRepository podcastRepository;

    @Transactional
    public void markFileUploadStarted(UUID podcastId) {
        PodcastEntity podcast = findForUpdate(podcastId);
        if (FILE_START_TERMINAL_STATUSES.contains(podcast.getStatus())) {
            log.info(
                    "Podcast file start_upload ignored because current status is terminal for upload start, podcastId={}, status={}",
                    podcastId,
                    podcast.getStatus()
            );
            return;
        }

        podcast.setStatus(Status.PROCESSING);
        podcastRepository.saveAndFlush(podcast);
        log.info("Podcast file upload started, podcastId={}, status={}", podcastId, Status.PROCESSING);
    }

    @Transactional
    public void markFileUploaded(UUID podcastId, String audioUrlFile, Long audioSizeFile) {
        PodcastEntity podcast = findForUpdate(podcastId);
        String normalizedAudioUrlFile = normalizeMediaPath(audioUrlFile, "audio_url_file");
        Long normalizedAudioSizeFile = normalizeAudioSize(audioSizeFile);

        podcast.setAudioUrlFile(normalizedAudioUrlFile);
        podcast.setAudioUrl(normalizedAudioUrlFile);
        podcast.setAudioSizeFile(normalizedAudioSizeFile);

        if (podcast.getStatus() != Status.PUBLISHED && podcast.getStatus() != Status.ARCHIVED) {
            podcast.setStatus(Status.READY_TO_PUBLISH);
        }

        podcastRepository.saveAndFlush(podcast);
        log.info(
                "Podcast file uploaded, podcastId={}, status={}, audioSizeFile={}",
                podcastId,
                podcast.getStatus(),
                normalizedAudioSizeFile
        );
    }

    @Transactional
    public void markFileUploadError(UUID podcastId, String errorMessage) {
        PodcastEntity podcast = findForUpdate(podcastId);
        if (FILE_ERROR_TERMINAL_STATUSES.contains(podcast.getStatus())) {
            log.warn(
                    "Podcast file upload error ignored because current status is terminal for upload error, podcastId={}, status={}, errorMessage={}",
                    podcastId,
                    podcast.getStatus(),
                    safeError(errorMessage)
            );
            return;
        }

        podcast.setStatus(Status.UPLOAD_ERROR);
        podcastRepository.saveAndFlush(podcast);
        log.warn("Podcast file upload failed, podcastId={}, status={}, errorMessage={}",
                podcastId, Status.UPLOAD_ERROR, safeError(errorMessage));
    }

    @Transactional
    public void updateCoverFromMediaEvent(UUID podcastId, String coverImageUrl) {
        PodcastEntity podcast = findForUpdate(podcastId);
        podcast.setCoverImageUrl(normalizeMediaPath(coverImageUrl, "podcast cover"));
        podcastRepository.saveAndFlush(podcast);
        log.info("Podcast cover updated from Kafka media event, podcastId={}", podcastId);
    }

    public void logCoverUploadError(UUID podcastId, String errorMessage) {
        if (podcastId == null) {
            throw new InvalidKafkaMessageException("Received null podcast cover object_id in Kafka");
        }
        log.warn("Podcast cover upload failed, podcastId={}, errorMessage={}", podcastId, safeError(errorMessage));
    }

    private PodcastEntity findForUpdate(UUID podcastId) {
        if (podcastId == null) {
            throw new InvalidKafkaMessageException("Received null podcast object_id in Kafka");
        }

        return podcastRepository.findDetailedByIdForUpdate(podcastId)
                .orElseThrow(() -> new KafkaRetryableProcessingException(
                        "Podcast not found for media event, podcastId=" + podcastId,
                        null
                ));
    }

    private String normalizeMediaPath(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidKafkaMessageException("Received blank " + fieldName + " in Kafka");
        }
        return value.trim();
    }

    private Long normalizeAudioSize(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new InvalidKafkaMessageException("Received negative audio_size_file in Kafka");
        }
        return value;
    }

    private String safeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }
}
