package com.linqibin.blog.post.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.linqibin.blog.post.domain.PostStatus;

// 文章持久化实体：只描述 posts 表怎么存，不承载领域行为。
@Entity
@Table(name = "posts")
public class PostEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120, unique = true)
    private String slug;

    @Column(name = "markdown_content", nullable = false, columnDefinition = "text")
    private String markdownContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status_before_trash", length = 20)
    private PostStatus previousStatusBeforeTrash;

    // 版本号：由领域对象 Post 在状态转换时递增，JPA 只负责持久化，不自动管理。
    // 并发冲突检测由 PostService 在应用层完成，保持内存模式和 JPA 模式行为一致。
    @Column(name = "version", nullable = false)
    private long version;

    protected PostEntity() {
        // JPA 通过反射创建实体时需要无参构造器。
    }

    public PostEntity(
            UUID id,
            String title,
            String slug,
            String markdownContent,
            PostStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            PostStatus previousStatusBeforeTrash,
            long version
    ) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.markdownContent = markdownContent;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.previousStatusBeforeTrash = previousStatusBeforeTrash;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public PostStatus getPreviousStatusBeforeTrash() {
        return previousStatusBeforeTrash;
    }

    public long getVersion() {
        return version;
    }
}
