package podcastService.podcast.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePodcastRequest(
        @NotBlank(message = "title must not be blank")
        @NotNull
        @Size(min = 3, max = 255, message = "title must be between 3 and 255 characters")
        String title,

        @Size(max = 5000, message = "the description cannot be longer than 5000 characters")
        String description,

        UUID  categoryId,
        String coverImageUrl,

        @JsonProperty("num_speakers")
        @JsonAlias("numSpeakers")
        @NotNull(message = "num_speakers must not be null")
        @Min(value = 1, message = "num_speakers must be greater than 0")
        @Max(value = 32, message = "num_speakers must be less than or equal to 32")
        Integer numSpeakers
) {
}
