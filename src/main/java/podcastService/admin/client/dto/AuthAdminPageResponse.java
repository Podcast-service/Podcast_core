package podcastService.admin.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthAdminPageResponse<T>(
        List<T> items,
        int page,
        int size,
        @JsonAlias("total_elements")
        long totalElements,
        @JsonAlias("total_pages")
        int totalPages
) {
}
