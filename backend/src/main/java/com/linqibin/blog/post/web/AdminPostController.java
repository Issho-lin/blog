package com.linqibin.blog.post.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;

// 管理端文章接口：按文章管理场景暴露接口
@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    // 管理端控制器只依赖应用层，不直接碰仓库或领域细节。
    private final PostService postService;
    private final PostDetailAssembler postDetailAssembler;

    public AdminPostController(PostService postService, PostDetailAssembler postDetailAssembler) {
        this.postService = postService;
        this.postDetailAssembler = postDetailAssembler;
    }

    @PostMapping("/drafts")
    public ResponseEntity<PostResponse> createDraft(@Valid @RequestBody CreatePostRequest request) {
        // Controller 只负责收参与出参转换，真正的创建流程放在 PostService 中。
        Post createdPost = postService.createDraft(request.title(), request.markdownContent(), request.slug(),
                request.categoryId(), request.tagIds(), request.excerpt(), request.coverUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(createdPost));
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(@PathVariable UUID postId) {
        // 编辑器加载文章时调用，返回完整文章数据（含版本号）供前端管理自动保存。
        Post post = postService.getPost(postId);
        return PostResponse.from(post);
    }

    @GetMapping("/{postId}/preview")
    public AdminPostPreviewResponse preview(@PathVariable UUID postId) {
        // 用与公开页相同的渲染结果做预览，不改变状态、不增加阅读数。
        return postDetailAssembler.toAdminPreview(postService.getPost(postId));
    }

    @GetMapping("/{postId}/save-status")
    public SaveStatusResponse getSaveStatus(@PathVariable UUID postId) {
        // 轻量查询：只返回版本号、更新时间和状态，前端用于确认服务端最新版本。
        Post post = postService.getSaveStatus(postId);
        return SaveStatusResponse.from(post);
    }

    @GetMapping
    public List<PostResponse> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID tagId
    ) {
        // 管理端列表支持按标题、分类和标签过滤，不传条件时返回全部文章。
        return postService.searchAdminPosts(keyword, categoryId, tagId).stream()
                .map(PostResponse::from)
                .toList();
    }

    @PostMapping("/batch-unpublish")
    public BatchPostActionResponse batchUnpublish(@Valid @RequestBody BatchPostIdsRequest request) {
        return BatchPostActionResponse.from(postService.batchUnpublish(request.ids()));
    }

    @PostMapping("/batch-trash")
    public BatchPostActionResponse batchMoveToTrash(@Valid @RequestBody BatchPostIdsRequest request) {
        return BatchPostActionResponse.from(postService.batchMoveToTrash(request.ids()));
    }

    @PutMapping("/{postId}")
    public PostResponse updatePost(@PathVariable UUID postId, @Valid @RequestBody UpdatePostRequest request) {
        // 编辑接口允许修改文章内容；是否能改、改完状态是否保持不变，由应用层和领域层决定。
        Post updatedPost = postService.updatePost(postId, request.title(), request.markdownContent(), request.slug(),
                request.categoryId(), request.tagIds(), request.expectedVersion(), request.excerpt(), request.coverUrl());
        return PostResponse.from(updatedPost);
    }

    @PostMapping("/{postId}/publish")
    public PostResponse publish(@PathVariable UUID postId) {
        // 发布接口只表达 HTTP 入口，业务规则仍由应用层和领域层负责。
        Post publishedPost = postService.publish(postId);
        return PostResponse.from(publishedPost);
    }

    @PostMapping("/{postId}/unpublish")
    public PostResponse unpublish(@PathVariable UUID postId) {
        // 下线后的状态会变成 UNPUBLISHED，便于后续再次发布。
        Post unpublishedPost = postService.unpublish(postId);
        return PostResponse.from(unpublishedPost);
    }

    @PostMapping("/{postId}/trash")
    public PostResponse moveToTrash(@PathVariable UUID postId) {
        // 回收站操作是管理端动作，所以也挂在 admin 路径下。
        Post trashedPost = postService.moveToTrash(postId);
        return PostResponse.from(trashedPost);
    }

    @PostMapping("/{postId}/restore")
    public PostResponse restoreFromTrash(@PathVariable UUID postId) {
        // 恢复后的目标状态由领域对象自己决定，控制器只返回结果。
        Post restoredPost = postService.restoreFromTrash(postId);
        return PostResponse.from(restoredPost);
    }

    @DeleteMapping("/{postId}")
    public void permanentlyDelete(@PathVariable UUID postId) {
        postService.permanentlyDelete(postId);
    }
}
