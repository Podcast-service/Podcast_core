package podcastService.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaJsonMessageParserTest {

    private static final KafkaRecordContext CONTEXT = new KafkaRecordContext(
            "topic", 0, 1L, "key", "corr", "msg"
    );

    private final KafkaJsonMessageParser parser = new KafkaJsonMessageParser(new ObjectMapper());

    @Test
    void parsesPayloadWithLeadingBom() {
        assertThat(parser.parse("\uFEFF{\"event\":\"uploaded\"}", CONTEXT).get("event").asText())
                .isEqualTo("uploaded");
    }
}
