package com.linqibin.blog.post.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

// 管理端批量操作请求：一次提交一组文章 ID。
public record BatchPostIdsRequest(
        @NotEmpty(message = "请选择文章")
        @Size(max = 50, message = "一次最多操作 50 篇文章")
        List<UUID> ids
) {
}
