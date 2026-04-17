package podcastService.category.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        String name,

        @PositiveOrZero(message = "position must be positive or zero")
        Integer position
) {
}
