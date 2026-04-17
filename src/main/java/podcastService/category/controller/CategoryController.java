package podcastService.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import podcastService.category.dto.CategoryResponse;
import podcastService.category.dto.CreateCategoryRequest;
import podcastService.category.dto.UpdateCategoryRequest;
import podcastService.category.service.CategoryService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryResponse> findCategories() {
        log.info("GET /categories");
        return categoryService.findCategories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        log.info("POST /categories");
        return categoryService.createCategory(request);
    }

    @PutMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryResponse updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        log.info("PUT /categories/{}", categoryId);
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID categoryId) {
        log.info("DELETE /categories/{}", categoryId);
        categoryService.deleteCategory(categoryId);
    }
}
