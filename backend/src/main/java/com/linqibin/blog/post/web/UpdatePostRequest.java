package com.linqibin.blog.post.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 编辑文章请求对象：更新标题、正文、可选 slug、分类和标签。
public record UpdatePostRequest(
        // 编辑文章时标题依然是必填项。
        @NotBlank(message = "标题不能为空")
        String title,
        // 草稿和已下线文章允许先把正文留空；已发布文章的限制交给领域层判断。
        String markdownContent,
        // 不传表示保留原 slug；传空字符串表示根据最新标题重新生成。
        String slug,
        // 不传表示保留原分类；传 null 值表示清除分类。
        UUID categoryId,
        // 不传表示保留原标签；传空列表表示清除所有标签。
        List<UUID> tagIds,
        // 乐观锁版本号：客户端从上次加载的文章中获取 version 值，提交时回传。
        // 不传（null）时跳过版本检查，仅用于不关心并发安全的场景。
        Long expectedVersion,
        // 不传表示保留原摘要；传空字符串表示清除，公开端回退到正文截取。
        @Size(max = 500, message = "摘要不能超过 500 个字符")
        String excerpt,
        // 不传表示保留原封面；传空字符串表示清除。
        @Size(max = 500, message = "封面地址不能超过 500 个字符")
        String coverUrl
) {
}
