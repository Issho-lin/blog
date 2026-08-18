package com.linqibin.blog.post.application;

import com.linqibin.blog.post.domain.Post;

// 按发布时间排序后的相邻已发布文章：previous 更早，next 更晚。
public record AdjacentPublishedPosts(Post previous, Post next) {
}
