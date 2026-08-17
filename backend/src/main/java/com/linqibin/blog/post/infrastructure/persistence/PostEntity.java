package com.linqibin.blog.post.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @Column(length = 500)
    private String excerpt;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "markdown_content", nullable = false, columnDefinition = "text")
    private String markdownContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    // 文章所属分类，可以为空（未分类）。
    @Column(name = "category_id")
    private UUID categoryId;

    // 文章关联的标签 ID 列表，通过 post_tags 关联表管理多对多关系。
    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag_id")
    private List<UUID> tagIds = new ArrayList<>();

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

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    protected PostEntity() {
        // JPA 通过反射创建实体时需要无参构造器。
    }

    public PostEntity(
            UUID id,
            String title,
            String slug,
            String excerpt,
            String coverUrl,
            String markdownContent,
            PostStatus status,
            UUID categoryId,
            List<UUID> tagIds,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            PostStatus previousStatusBeforeTrash,
            long version,
            long viewCount
    ) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.excerpt = excerpt;
        this.coverUrl = coverUrl;
        this.markdownContent = markdownContent;
        this.status = status;
        this.categoryId = categoryId;
        this.tagIds = tagIds != null ? new ArrayList<>(tagIds) : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.previousStatusBeforeTrash = previousStatusBeforeTrash;
        this.version = version;
        this.viewCount = viewCount;
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

    public String getExcerpt() {
        return excerpt;
    }

    public String getCoverUrl() {
        return coverUrl;
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

    public long getViewCount() {
        return viewCount;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public List<UUID> getTagIds() {
        return tagIds;
    }
}
