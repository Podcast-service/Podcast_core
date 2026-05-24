package podcastService.media.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaEventParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MediaEventParser parser = new MediaEventParser();

    @Test
    void parsesBaseMediaEventWithoutBindingToFullSchema() throws Exception {
        MediaEvent event = parser.parse(objectMapper.readTree("""
                {
                  "type": "podcast_file",
                  "event": "uploaded",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "audio_url_file": "/media/audio.mp3",
                  "future_field": {"version": 2}
                }
                """));

        assertThat(event.key()).isEqualTo(new MediaEventKey("podcast_file", "uploaded"));
        assertThat(event.objectId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000301"));
        assertThat(event.requiredText("audio_url_file")).isEqualTo("/media/audio.mp3");
        assertThat(event.rawPayload().get("future_field").get("version").asInt()).isEqualTo(2);
    }

    @Test
    void resolvesFirstAvailableTextFieldForEvolvingMediaContract() throws Exception {
        MediaEvent event = parser.parse(objectMapper.readTree("""
                {
                  "type": "podcast_cover",
                  "event": "uploaded",
                  "object_id": "00000000-0000-0000-0000-000000000301",
                  "file_url": "/media/covers/podcast.png"
                }
                """));

        assertThat(event.requiredAnyText("cover_url", "file_url", "url")).isEqualTo("/media/covers/podcast.png");
    }

    @Test
    void rejectsEventWithoutObjectId() throws Exception {
        assertThatThrownBy(() -> parser.parse(objectMapper.readTree("""
                {
                  "type": "podcast_file",
                  "event": "uploaded"
                }
                """)))
                .isInstanceOf(KafkaMessageValidationException.class);
    }
}
