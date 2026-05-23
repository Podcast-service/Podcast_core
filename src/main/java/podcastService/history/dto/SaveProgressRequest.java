package podcastService.history.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveProgressRequest(
        @NotNull(message = "progressSeconds must not be null")
        @Min(value = 0, message = "progressSeconds must be greater than or equal to 0")
        Integer progressSeconds
) {
}
