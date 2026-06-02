package podcastService.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminRoleRequest(
        @NotBlank
        @Pattern(regexp = "user|author|admin")
        String roleName
) {
}
