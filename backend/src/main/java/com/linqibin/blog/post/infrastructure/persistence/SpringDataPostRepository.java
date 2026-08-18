package com.linqibin.blog.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.linqibin.blog.post.domain.PostStatus;

public interface SpringDataPostRepository extends JpaRepository<PostEntity, UUID> {

    Optional<PostEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT e FROM PostEntity e WHERE e.status = :status ORDER BY e.publishedAt DESC")
    List<PostEntity> findByStatus(PostStatus status, Pageable pageable);

    long countByStatus(PostStatus status);

    @Query("SELECT e FROM PostEntity e WHERE e.status = :status AND e.categoryId = :categoryId ORDER BY e.publishedAt DESC")
    List<PostEntity> findPublishedByCategory(@Param("status") PostStatus status, @Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM PostEntity e WHERE e.status = :status AND e.categoryId = :categoryId")
    long countPublishedByCategory(@Param("status") PostStatus status, @Param("categoryId") UUID categoryId);

    @Query("SELECT e FROM PostEntity e WHERE e.status = :status AND :tagId MEMBER OF e.tagIds ORDER BY e.publishedAt DESC")
    List<PostEntity> findPublishedByTag(@Param("status") PostStatus status, @Param("tagId") UUID tagId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM PostEntity e WHERE e.status = :status AND :tagId MEMBER OF e.tagIds")
    long countPublishedByTag(@Param("status") PostStatus status, @Param("tagId") UUID tagId);

    @Query("SELECT e FROM PostEntity e WHERE e.status = :status AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.markdownContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (e.excerpt IS NOT NULL AND LOWER(e.excerpt) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR (e.seoTitle IS NOT NULL AND LOWER(e.seoTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR (e.seoDescription IS NOT NULL AND LOWER(e.seoDescription) LIKE LOWER(CONCAT('%', :keyword, '%')))) ORDER BY e.publishedAt DESC")
    List<PostEntity> searchPublished(@Param("status") PostStatus status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(e) FROM PostEntity e WHERE e.status = :status AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.markdownContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (e.excerpt IS NOT NULL AND LOWER(e.excerpt) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR (e.seoTitle IS NOT NULL AND LOWER(e.seoTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR (e.seoDescription IS NOT NULL AND LOWER(e.seoDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    long countSearchPublished(@Param("status") PostStatus status, @Param("keyword") String keyword);
}
