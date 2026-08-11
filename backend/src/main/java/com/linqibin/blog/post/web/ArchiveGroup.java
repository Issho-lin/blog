package com.linqibin.blog.post.web;

import java.util.List;

// 归档分组：按年月组织已发布文章，供前端渲染归档页面。
public record ArchiveGroup(
        int year,
        int month,
        List<ArchiveItem> items
) {
}
