package podcastService.media.messaging.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaHandlerKey;
import podcastService.media.messaging.MediaWorkerEventHandler;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaWorkerEventDto;
import podcastService.media.messaging.contract.MediaWorkerEventType;
import podcastService.podcast.service.PodcastMediaMetadataService;

@Component
@RequiredArgsConstructor
public class PodcastProcessingFailedHandler implements MediaWorkerEventHandler {

    private static final MediaHandlerKey<MediaWorkerEventType> KEY =
            new MediaHandlerKey<>(MediaObjectType.PODCAST_FILE_URL, MediaWorkerEventType.PROCESSING_FAILED);

    private final PodcastMediaMetadataService service;

    @Override
    public MediaHandlerKey<MediaWorkerEventType> key() {
        return KEY;
    }

    @Override
    public void handle(MediaWorkerEventDto event, KafkaRecordContext context) {
        service.markFailed(event.targetPodcastId(context.keyAsUuidOrNull()), event.error(), event.timestamp(), "media.worker");
    }
}
