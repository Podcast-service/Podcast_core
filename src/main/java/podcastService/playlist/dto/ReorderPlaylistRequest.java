package podcastService.playlist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderPlaylistRequest(
        @NotEmpty(message = "items must not be empty")
        List<@Valid Item> items
) {
    public record Item(
            @NotNull(message = "podcastId must not be null")
            UUID podcastId,

            @Min(value = 1, message = "position must be greater than or equal to 1")
            int position
    ) {
    }
}
