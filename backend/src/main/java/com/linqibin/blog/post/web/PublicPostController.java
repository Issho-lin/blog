package com.linqibin.blog.post.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;

// 公开文章接口：负责根据 slug 返回文章详情，供前台页面读取。
@RestController
@RequestMapping("/api/public/posts")
public class PublicPostController {

    private final PostService postService;

    public PublicPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{slug}")
    public PostResponse getPostBySlug(@PathVariable String slug) {
        // 先从应用层拿到领域对象，再映射成给前端的响应对象。
        Post post = postService.getPostBySlug(slug);
        return PostResponse.from(post);
    }
}
