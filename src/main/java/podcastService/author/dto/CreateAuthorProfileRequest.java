package podcastService.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuthorProfileRequest(
        @NotBlank(message = "authorName must not be blank")
        @Size(min = 2, max = 100, message = "authorName must be between 2 and 100 characters")
        String authorName,

        @Size(max = 1000, message = "description must not be longer than 1000 characters")
        String description
) {
}
