package podcastService.podcast.dto;

import jakarta.validation.constraints.NotBlank;
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
        String coverImageUrl
) {
}
