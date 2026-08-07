package com.linqibin.blog.post.web;

import jakarta.validation.constraints.NotBlank;

// 编辑文章请求对象：更新标题、正文，以及可选的 slug 调整。
public record UpdatePostRequest(
        // 编辑文章时标题依然是必填项。
        @NotBlank(message = "标题不能为空")
        String title,
        // 草稿和已下线文章允许先把正文留空；已发布文章的限制交给领域层判断。
        String markdownContent,
        // 不传表示保留原 slug；传空字符串表示根据最新标题重新生成。
        String slug,
        // 乐观锁版本号：客户端从上次加载的文章中获取 version 值，提交时回传。
        // 不传（null）时跳过版本检查，仅用于不关心并发安全的场景。
        Long expectedVersion
) {
}
