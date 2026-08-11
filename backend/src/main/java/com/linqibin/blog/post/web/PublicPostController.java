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

    // 公开接口同样只依赖应用层，由它负责查找文章和抛业务异常。
    private final PostService postService;

    public PublicPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{slug}")
    public PostResponse getPostBySlug(@PathVariable String slug) {
        // 公开接口只返回已发布文章，草稿和下线文章对访客不可见。
        Post post = postService.getPublishedPostBySlug(slug);
        return PostResponse.from(post);
    }
}
