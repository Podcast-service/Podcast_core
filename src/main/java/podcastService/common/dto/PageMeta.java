package podcastService.common.dto;

public record PageMeta(
       int page,
       int size,
       long totalElements,
       int totalPages
) {
}
