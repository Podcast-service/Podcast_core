package podcastService.category.mapper;


import org.springframework.stereotype.Component;
import podcastService.category.dto.CategoryResponse;
import podcastService.category.entity.CategoryEntity;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(CategoryEntity entity) {
        return new CategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getPosition()
        );
    }
}
