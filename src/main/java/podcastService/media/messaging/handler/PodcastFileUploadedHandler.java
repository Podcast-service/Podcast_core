package podcastService.media.messaging.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaEvent;
import podcastService.media.messaging.MediaEventHandler;
import podcastService.media.messaging.MediaEventKey;
import podcastService.podcast.service.PodcastMediaMetadataService;

@Component
@RequiredArgsConstructor
public class PodcastFileUploadedHandler implements MediaEventHandler {

    private static final MediaEventKey KEY = new MediaEventKey("podcast_file", "uploaded");

    private final PodcastMediaMetadataService podcastMediaMetadataService;

    @Override
    public MediaEventKey key() {
        return KEY;
    }

    @Override
    public void handle(MediaEvent event, KafkaRecordContext context) {
        podcastMediaMetadataService.markFileUploaded(
                event.objectId(),
                event.requiredText("audio_url_file"),
                event.optionalLong("audio_size_file")
        );
    }
}
