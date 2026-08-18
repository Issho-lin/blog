package com.linqibin.blog.post.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostEntityMapperTest {

    private final PostEntityMapper mapper = new PostEntityMapper();

    @Test
    void toEntityCopiesAllDomainFields() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant createdAt = Instant.parse("2026-08-05T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-05T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-08-05T11:00:00Z");
        Post post = new Post(
                id,
                "Hello JPA",
                "hello-jpa",
                "Custom excerpt",
                "/uploads/cover.jpg",
                "SEO Title",
                "SEO Description",
                "# content",
                PostStatus.TRASHED,
                null,
                null,
                createdAt,
                updatedAt,
                publishedAt,
                PostStatus.UNPUBLISHED,
                3L,
                0L
        );

        PostEntity entity = mapper.toEntity(post);

        assertEquals(id, entity.getId());
        assertEquals("Hello JPA", entity.getTitle());
        assertEquals("hello-jpa", entity.getSlug());
        assertEquals("Custom excerpt", entity.getExcerpt());
        assertEquals("/uploads/cover.jpg", entity.getCoverUrl());
        assertEquals("SEO Title", entity.getSeoTitle());
        assertEquals("SEO Description", entity.getSeoDescription());
        assertEquals("# content", entity.getMarkdownContent());
        assertEquals(PostStatus.TRASHED, entity.getStatus());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
        assertEquals(publishedAt, entity.getPublishedAt());
        assertEquals(PostStatus.UNPUBLISHED, entity.getPreviousStatusBeforeTrash());
        assertEquals(3L, entity.getVersion());
        assertEquals(0L, entity.getViewCount());
    }

    @Test
    void toDomainRestoresAllEntityFields() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Instant createdAt = Instant.parse("2026-08-05T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-05T10:00:00Z");
        PostEntity entity = new PostEntity(
                id,
                "Draft Post",
                "draft-post",
                null,
                null,
                null,
                null,
                "",
                PostStatus.DRAFT,
                null,
                null,
                createdAt,
                updatedAt,
                null,
                null,
                0L,
                0L
        );

        Post post = mapper.toDomain(entity);

        assertEquals(id, post.id());
        assertEquals("Draft Post", post.title());
        assertEquals("draft-post", post.slug());
        assertNull(post.excerpt());
        assertNull(post.coverUrl());
        assertNull(post.seoTitle());
        assertNull(post.seoDescription());
        assertEquals("", post.markdownContent());
        assertEquals(PostStatus.DRAFT, post.status());
        assertEquals(createdAt, post.createdAt());
        assertEquals(updatedAt, post.updatedAt());
        assertNull(post.publishedAt());
        assertNull(post.previousStatusBeforeTrash());
        assertEquals(0L, post.version());
        assertEquals(0L, post.viewCount());
    }
}
