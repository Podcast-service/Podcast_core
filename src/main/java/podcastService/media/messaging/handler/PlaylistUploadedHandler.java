package podcastService.media.messaging.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaHandlerKey;
import podcastService.media.messaging.MediaUploadEventHandler;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventDto;
import podcastService.media.messaging.contract.MediaUploadEventType;
import podcastService.playlist.service.PlaylistMediaService;

@Component
@RequiredArgsConstructor
public class PlaylistUploadedHandler implements MediaUploadEventHandler {

    private static final MediaHandlerKey<MediaUploadEventType> KEY =
            new MediaHandlerKey<>(MediaObjectType.PLAYLIST, MediaUploadEventType.UPLOADED);

    private final PlaylistMediaService service;

    @Override
    public MediaHandlerKey<MediaUploadEventType> key() {
        return KEY;
    }

    @Override
    public void handle(MediaUploadEventDto event, KafkaRecordContext context) {
        service.updateCoverFromMediaEvent(event.objectId(), event.audioUrlFile());
    }
}
