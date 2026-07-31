package com.linqibin.blog.post.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;

// 管理端文章接口：负责接收创建请求，并把请求转交给应用层完成草稿创建。
@RestController
@RequestMapping("/api/admin/posts")
public class AdminPostController {

    private final PostService postService;

    public AdminPostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createDraft(@Valid @RequestBody CreatePostRequest request) {
        // Controller 只负责收参与出参转换，真正的创建流程放在 PostService 中。
        Post createdPost = postService.createDraft(request.title(), request.markdownContent(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(createdPost));
    }
}
