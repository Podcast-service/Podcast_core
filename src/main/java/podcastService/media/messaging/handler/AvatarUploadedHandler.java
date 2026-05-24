package podcastService.media.messaging.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaEvent;
import podcastService.media.messaging.MediaEventHandler;
import podcastService.media.messaging.MediaEventKey;
import podcastService.user.service.UserProfileService;

@Component
@RequiredArgsConstructor
public class AvatarUploadedHandler implements MediaEventHandler {

    private static final MediaEventKey KEY = new MediaEventKey("avatar", "uploaded");

    private final UserProfileService userProfileService;

    @Override
    public MediaEventKey key() {
        return KEY;
    }

    @Override
    public void handle(MediaEvent event, KafkaRecordContext context) {
        userProfileService.updateAvatarFromMediaEvent(
                event.objectId(),
                event.requiredAnyText("avatar_url", "image_url", "file_url", "url", "audio_url_file"));
    }
}
