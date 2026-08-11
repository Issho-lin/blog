package com.linqibin.blog.post.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 文章仓库抽象：领域层只定义需要什么持久化能力，不关心底层怎么存。

public interface PostRepository {

    // 保存文章的新状态，创建和更新都走这个入口。
    Post save(Post post);

    // 按主键查询文章，方便管理端做状态流转。
    Optional<Post> findById(UUID id);

    // 按 slug 查询文章，主要给公开阅读接口使用。
    Optional<Post> findBySlug(String slug);

    // 判断 slug 是否已被占用，给创建文章时做唯一性校验。
    boolean existsBySlug(String slug);

    // 返回全部文章，后续可用于管理端列表或搜索。
    List<Post> findAll();

    // 分页返回已发布文章，按发布时间倒序排列。
    List<Post> findPublishedPosts(int page, int pageSize);

    // 统计已发布文章总数。
    long countPublishedPosts();

    // 按分类分页查询已发布文章。
    List<Post> findPublishedPostsByCategory(UUID categoryId, int page, int pageSize);

    // 统计指定分类下已发布文章总数。
    long countPublishedPostsByCategory(UUID categoryId);

    // 按标签分页查询已发布文章。
    List<Post> findPublishedPostsByTag(UUID tagId, int page, int pageSize);

    // 统计指定标签下已发布文章总数。
    long countPublishedPostsByTag(UUID tagId);

    // 按关键词搜索已发布文章（匹配标题和正文），分页返回。
    List<Post> searchPublishedPosts(String keyword, int page, int pageSize);

    // 统计关键词搜索匹配的已发布文章总数。
    long countSearchPublishedPosts(String keyword);
}
