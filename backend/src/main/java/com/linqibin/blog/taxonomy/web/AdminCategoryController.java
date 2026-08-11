package com.linqibin.blog.taxonomy.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.domain.Category;

// 管理端分类接口：分类的增删改查。
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.create(request.name(), request.slug(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable UUID id) {
        return CategoryResponse.from(categoryService.getCategory(id));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        Category category = categoryService.update(id, request.name(), request.description());
        return CategoryResponse.from(category);
    }

    @PutMapping("/{id}/slug")
    public CategoryResponse updateSlug(@PathVariable UUID id, @RequestBody String slug) {
        Category category = categoryService.updateSlug(id, slug);
        return CategoryResponse.from(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
