package podcastService.podcast.dto;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.config.JacksonConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreatePodcastRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsSnakeCaseNumSpeakersFromJson() throws Exception {
        CreatePodcastRequest request = new JacksonConfig().objectMapper().readValue("""
                {
                  "title": "Выпуск про Kafka",
                  "num_speakers": 2
                }
                """, CreatePodcastRequest.class);

        assertThat(request.numSpeakers()).isEqualTo(2);
    }

    @Test
    void rejectsMissingNumSpeakers() {
        CreatePodcastRequest request = new CreatePodcastRequest("Выпуск про Kafka", null, null, null, null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("numSpeakers"));
    }

    @Test
    void rejectsZeroNumSpeakers() {
        CreatePodcastRequest request = new CreatePodcastRequest("Выпуск про Kafka", null, null, null, 0);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage().equals("num_speakers must be greater than 0"));
    }

    @Test
    void rejectsNegativeNumSpeakers() {
        CreatePodcastRequest request = new CreatePodcastRequest("Выпуск про Kafka", null, null, null, -1);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage().equals("num_speakers must be greater than 0"));
    }

    @Test
    void rejectsTooLargeNumSpeakers() {
        CreatePodcastRequest request = new CreatePodcastRequest("Выпуск про Kafka", null, null, null, 33);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage().equals("num_speakers must be less than or equal to 32"));
    }

    @Test
    void rejectsNonNumericNumSpeakers() {
        assertThatThrownBy(() -> new JacksonConfig().objectMapper().readValue("""
                {
                  "title": "Выпуск про Kafka",
                  "num_speakers": "two"
                }
                """, CreatePodcastRequest.class))
                .isInstanceOf(MismatchedInputException.class);
    }
}
