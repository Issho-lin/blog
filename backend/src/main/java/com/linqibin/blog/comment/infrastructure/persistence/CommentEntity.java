package com.linqibin.blog.comment.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class CommentEntity {

    @Id
    private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "author_name", nullable = false, length = 40)
    private String authorName;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CommentEntity() {
    }

    public CommentEntity(
            UUID id,
            UUID postId,
            String authorName,
            String content,
            String ip,
            Instant createdAt
    ) {
        this.id = id;
        this.postId = postId;
        this.authorName = authorName;
        this.content = content;
        this.ip = ip;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPostId() {
        return postId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public String getIp() {
        return ip;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
