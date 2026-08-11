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

import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Tag;

// 管理端标签接口：标签的增删改查。
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody CreateTagRequest request) {
        Tag tag = tagService.create(request.name(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(tag));
    }

    @GetMapping
    public List<TagResponse> list() {
        return tagService.findAll().stream()
                .map(TagResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TagResponse get(@PathVariable UUID id) {
        return TagResponse.from(tagService.getTag(id));
    }

    @PutMapping("/{id}")
    public TagResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTagRequest request) {
        Tag tag = tagService.update(id, request.name());
        return TagResponse.from(tag);
    }

    @PutMapping("/{id}/slug")
    public TagResponse updateSlug(@PathVariable UUID id, @RequestBody String slug) {
        Tag tag = tagService.updateSlug(id, slug);
        return TagResponse.from(tag);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
