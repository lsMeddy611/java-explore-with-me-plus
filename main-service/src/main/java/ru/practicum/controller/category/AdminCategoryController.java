package ru.practicum.controller.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.service.category.CategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody NewCategoryDto newCategoryDto) {

        log.info("POST /admin/categories : {}", newCategoryDto.name());

        CategoryDto createdCategory = categoryService.createCategory(newCategoryDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long catId,
            @Valid @RequestBody CategoryDto categoryDto) {

        log.info("PATCH /admin/categories/{}", catId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(categoryService.updateCategory(catId, categoryDto));
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long catId) {

        log.info("DELETE /admin/categories/{}", catId);

        categoryService.deleteCategory(catId);
        return ResponseEntity.noContent().build();
    }
}