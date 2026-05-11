package podcastService.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlaylistRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must not be longer than 255 characters")
        String title,

        @Size(max = 1000, message = "description must not be longer than 1000 characters")
        String description,

        @Size(max = 2048, message = "coverImageUrl must not be longer than 2048 characters")
        String coverImageUrl,

        Boolean isPublic
) {
}
