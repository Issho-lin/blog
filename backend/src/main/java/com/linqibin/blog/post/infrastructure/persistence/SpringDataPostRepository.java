package com.linqibin.blog.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.linqibin.blog.post.domain.PostStatus;

// Spring Data JPA 仓库：直接面向 PostEntity 提供查表能力。
public interface SpringDataPostRepository extends JpaRepository<PostEntity, UUID> {

    // 前台按 slug 读取文章时，底层最终会走到这个查询方法。
    Optional<PostEntity> findBySlug(String slug);

    // 创建或编辑文章时，用它判断 slug 是否已被其他记录占用。
    boolean existsBySlug(String slug);

    // 分页查询已发布文章，按发布时间倒序排列。
    @Query("SELECT e FROM PostEntity e WHERE e.status = :status ORDER BY e.publishedAt DESC")
    List<PostEntity> findByStatus(PostStatus status, Pageable pageable);

    // 统计已发布文章总数。
    long countByStatus(PostStatus status);

    // 按分类分页查询已发布文章。
    @Query("SELECT e FROM PostEntity e WHERE e.status = :status AND e.categoryId = :categoryId ORDER BY e.publishedAt DESC")
    List<PostEntity> findPublishedByCategory(@Param("status") PostStatus status, @Param("categoryId") UUID categoryId, Pageable pageable);

    // 统计指定分类下已发布文章总数。
    @Query("SELECT COUNT(e) FROM PostEntity e WHERE e.status = :status AND e.categoryId = :categoryId")
    long countPublishedByCategory(@Param("status") PostStatus status, @Param("categoryId") UUID categoryId);

    // 按标签分页查询已发布文章。
    @Query("SELECT e FROM PostEntity e WHERE e.status = :status AND :tagId MEMBER OF e.tagIds ORDER BY e.publishedAt DESC")
    List<PostEntity> findPublishedByTag(@Param("status") PostStatus status, @Param("tagId") UUID tagId, Pageable pageable);

    // 统计指定标签下已发布文章总数。
    @Query("SELECT COUNT(e) FROM PostEntity e WHERE e.status = :status AND :tagId MEMBER OF e.tagIds")
    long countPublishedByTag(@Param("status") PostStatus status, @Param("tagId") UUID tagId);

    // 按关键词搜索已发布文章（标题和正文 ILIKE 匹配），分页返回。
    @Query("SELECT e FROM PostEntity e WHERE e.status = :status AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.markdownContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (e.excerpt IS NOT NULL AND LOWER(e.excerpt) LIKE LOWER(CONCAT('%', :keyword, '%')))) ORDER BY e.publishedAt DESC")
    List<PostEntity> searchPublished(@Param("status") PostStatus status, @Param("keyword") String keyword, Pageable pageable);

    // 统计关键词搜索匹配的已发布文章总数。
    @Query("SELECT COUNT(e) FROM PostEntity e WHERE e.status = :status AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.markdownContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (e.excerpt IS NOT NULL AND LOWER(e.excerpt) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    long countSearchPublished(@Param("status") PostStatus status, @Param("keyword") String keyword);
}
