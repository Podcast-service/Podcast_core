package podcastService.media.messaging.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaHandlerKey;
import podcastService.media.messaging.MediaUploadEventHandler;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventDto;
import podcastService.media.messaging.contract.MediaUploadEventType;
import podcastService.podcast.service.PodcastMediaMetadataService;

@Component
@RequiredArgsConstructor
public class PodcastFileUploadedHandler implements MediaUploadEventHandler {

    private static final MediaHandlerKey<MediaUploadEventType> KEY =
            new MediaHandlerKey<>(MediaObjectType.PODCAST_FILE_URL, MediaUploadEventType.UPLOADED);

    private final PodcastMediaMetadataService service;

    @Override
    public MediaHandlerKey<MediaUploadEventType> key() {
        return KEY;
    }

    @Override
    public void handle(MediaUploadEventDto event, KafkaRecordContext context) {
        service.markFileUploaded(event.targetPodcastId(), event.audioUrlFile(), event.audioFileSize(), event.timestamp());
    }
}
