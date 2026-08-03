package com.linqibin.blog.post.domain;

// 文章生命周期里的几种核心状态。
public enum PostStatus {
    // 刚创建或编辑中的草稿，外部不可见。
    DRAFT,
    // 已公开发布，可以被前台读取。
    PUBLISHED,
    // 曾经发布过但当前已下线，保留内容等待再次发布。
    UNPUBLISHED,
    // 已移入回收站，不能直接对外展示。
    TRASHED
}
