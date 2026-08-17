package com.linqibin.blog.post.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.post.application.PostService;

// 管理控制台接口：聚合文章统计和最近动态，供 /admin 首页使用。
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final PostService postService;

    public AdminDashboardController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public AdminDashboardResponse getDashboard() {
        return AdminDashboardResponse.from(postService.getDashboard());
    }
}
