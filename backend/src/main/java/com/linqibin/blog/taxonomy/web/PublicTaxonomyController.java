package com.linqibin.blog.taxonomy.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;

// 公开分类和标签接口：访客可查看分类列表、标签列表和按 slug 查询。
@RestController
@RequestMapping("/api/public")
public class PublicTaxonomyController {

    private final CategoryService categoryService;
    private final TagService tagService;

    public PublicTaxonomyController(CategoryService categoryService, TagService tagService) {
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    // 公开分类列表：返回全部分类，供前台分类页使用。
    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    // 按 slug 查询分类详情。
    @GetMapping("/categories/{slug}")
    public CategoryResponse getCategoryBySlug(@PathVariable String slug) {
        return CategoryResponse.from(categoryService.getCategoryBySlug(slug));
    }

    // 公开标签列表：返回全部标签，供前台标签页使用。
    @GetMapping("/tags")
    public List<TagResponse> listTags() {
        return tagService.findAll().stream()
                .map(TagResponse::from)
                .toList();
    }

    // 按 slug 查询标签详情。
    @GetMapping("/tags/{slug}")
    public TagResponse getTagBySlug(@PathVariable String slug) {
        return TagResponse.from(tagService.getTagBySlug(slug));
    }
}
