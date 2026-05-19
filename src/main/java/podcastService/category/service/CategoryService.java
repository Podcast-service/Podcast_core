package podcastService.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.category.dto.CategoryResponse;
import podcastService.category.dto.CreateCategoryRequest;
import podcastService.category.dto.UpdateCategoryRequest;
import podcastService.category.entity.CategoryEntity;
import podcastService.category.mapper.CategoryMapper;
import podcastService.category.repository.CategoryRepository;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.BusinessRuleException;
import podcastService.common.exception.ConflictException;
import podcastService.common.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findCategories() {
        return categoryRepository.findAllByOrderByPositionAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        try {
            lockOrdering();

            int targetPosition = resolveCreatePosition(request.position());

            if (request.position() != null) {
                categoryRepository.shiftRightFrom(targetPosition);
            }

            CategoryEntity entity = new CategoryEntity();
            entity.setName(request.name().trim());
            entity.setPosition(targetPosition);

            CategoryEntity saved = categoryRepository.saveAndFlush(entity);

            log.info("Category created: id={}, name={}, position={}",
                    saved.getId(), saved.getName(), saved.getPosition());

            return categoryMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to create category: name={}, requestedPosition={}, exception message={}",
                    request.name(), request.position(), e.getMessage());

            throw new ConflictException(
                    "Failed to create category because of data conflict"
            );
        }
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        validateUpdateRequest(request);

        try {
            lockOrdering();

            CategoryEntity entity = categoryRepository.findByIdForUpdate(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));

            if (request.name() != null) {
                String trimmedName = request.name().trim();
                if (trimmedName.isEmpty()) {
                    throw new BadRequestException("name must not be blank");
                }
                entity.setName(trimmedName);
            }

            if (request.position() != null) {
                moveCategory(entity, request.position());
            }

            CategoryEntity saved = categoryRepository.saveAndFlush(entity);

            log.info("Category updated: id={}, name={}, position={}",
                    saved.getId(), saved.getName(), saved.getPosition());

            return categoryMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to update category: id={}, requestedPosition={}, exception message={}",
                    categoryId, request.position(), e.getMessage());

            throw new ConflictException(
                    "Failed to update category because of data conflict"
            );
        }
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        try {
            lockOrdering();

            CategoryEntity entity = categoryRepository.findByIdForUpdate(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));

            int deletedPosition = entity.getPosition();

            categoryRepository.delete(entity);
            categoryRepository.flush();

            categoryRepository.closeGapAfterDelete(deletedPosition);

            log.info("Category deleted: id={}, position={}", categoryId, deletedPosition);
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to delete category: id={}, exception message={}", categoryId, e.getMessage());

            throw new BusinessRuleException(
                    "Category cannot be deleted because it is in use"
            );
        }
    }

    private void lockOrdering() {
        categoryRepository.lockOrdering();
        categoryRepository.deferPositionConstraint();
    }

    private int resolveCreatePosition(Integer requestedPosition) {
        int maxPosition = categoryRepository.findTopByOrderByPositionDesc()
                .map(CategoryEntity::getPosition)
                .orElse(-1);

        if (requestedPosition == null) {
            return maxPosition + 1;
        }

        return Math.min(requestedPosition, maxPosition + 1);
    }

    private void moveCategory(CategoryEntity entity, int requestedPosition) {
        int oldPosition = entity.getPosition();

        int maxPosition = categoryRepository.findTopByOrderByPositionDesc()
                .map(CategoryEntity::getPosition)
                .orElse(0);

        int newPosition = Math.min(requestedPosition, maxPosition);

        if (newPosition == oldPosition) {
            return;
        }

        if (newPosition < oldPosition) {
            categoryRepository.shiftRightInRange(newPosition, oldPosition, entity.getId());
        } else {
            categoryRepository.shiftLeftInRange(oldPosition, newPosition, entity.getId());
        }

        entity.setPosition(newPosition);
    }

    private void validateUpdateRequest(UpdateCategoryRequest request) {
        if (request.name() == null && request.position() == null) {
            throw new BadRequestException("at least one field must be provided for update");
        }
    }
}
