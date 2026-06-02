package podcastService.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminRoleMutationResponse(
        UUID userId,
        List<String> roles,
        boolean changed
) {
}
