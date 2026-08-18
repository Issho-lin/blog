package com.linqibin.blog.post.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.linqibin.blog.post.domain.PostRevisionKind;

@Entity
@Table(name = "post_revisions")
public class PostRevisionEntity {

    @Id
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "markdown_content", nullable = false, columnDefinition = "text")
    private String markdownContent;

    @Column(length = 500)
    private String excerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostRevisionKind kind;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PostRevisionEntity() {
    }

    public PostRevisionEntity(
            UUID id,
            UUID postId,
            String title,
            String markdownContent,
            String excerpt,
            PostRevisionKind kind,
            Instant createdAt
    ) {
        this.id = id;
        this.postId = postId;
        this.title = title;
        this.markdownContent = markdownContent;
        this.excerpt = excerpt;
        this.kind = kind;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public PostRevisionKind getKind() {
        return kind;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
