package podcastService.user.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegisteredEventDtoTest {

    private static final KafkaRecordContext CONTEXT = new KafkaRecordContext(
            "podcast.user.register", 0, 1L, null, null, null
    );

    private final KafkaMessageReader reader = new KafkaMessageReader(new ObjectMapper());

    @Test
    void readsUserRegisterDto() {
        UserRegisteredEvent event = reader.read("""
                {
                  "user_id": "550e8400-e29b-41d4-a716-446655440000",
                  "username": "testuser",
                  "extra": "ignored"
                }
                """, UserRegisteredEvent.class, CONTEXT);

        assertThat(event.userId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(event.username()).isEqualTo("testuser");
    }
}
