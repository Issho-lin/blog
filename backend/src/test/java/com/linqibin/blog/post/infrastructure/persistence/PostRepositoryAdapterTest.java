package com.linqibin.blog.post.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostRepositoryAdapterTest {

    @Mock
    private SpringDataPostRepository springDataPostRepository;

    private PostRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PostRepositoryAdapter(springDataPostRepository, new PostEntityMapper());
    }

    @Test
    void saveDelegatesToSpringDataAndReturnsMappedDomainObject() {
        Post post = createPost(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "Hello Adapter",
                "hello-adapter",
                PostStatus.DRAFT
        );
        PostEntity savedEntity = new PostEntity(
                post.id(),
                post.title(),
                post.slug(),
                post.excerpt(),
                post.coverUrl(),
                post.seoTitle(),
                post.seoDescription(),
                post.markdownContent(),
                PostStatus.PUBLISHED,
                null,
                null,
                post.createdAt(),
                Instant.parse("2026-08-05T13:00:00Z"),
                Instant.parse("2026-08-05T13:00:00Z"),
                null,
                0L,
                0L
        );
        when(springDataPostRepository.save(any(PostEntity.class))).thenReturn(savedEntity);

        Post savedPost = adapter.save(post);

        assertEquals(PostStatus.PUBLISHED, savedPost.status());
        assertEquals(Instant.parse("2026-08-05T13:00:00Z"), savedPost.updatedAt());
        assertEquals(Instant.parse("2026-08-05T13:00:00Z"), savedPost.publishedAt());
        verify(springDataPostRepository).save(argThat(entity ->
                entity.getId().equals(post.id())
                        && entity.getTitle().equals(post.title())
                        && entity.getSlug().equals(post.slug())
                        && entity.getStatus() == post.status()
        ));
    }

    @Test
    void findBySlugMapsEntityBackToDomain() {
        PostEntity entity = new PostEntity(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "Read Me",
                "read-me",
                null,
                null,
                null,
                null,
                "# read",
                PostStatus.PUBLISHED,
                null,
                null,
                Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T10:00:00Z"),
                Instant.parse("2026-08-05T10:00:00Z"),
                null,
                0L,
                0L
        );
        when(springDataPostRepository.findBySlug("read-me")).thenReturn(Optional.of(entity));

        Optional<Post> foundPost = adapter.findBySlug("read-me");

        assertTrue(foundPost.isPresent());
        assertEquals("Read Me", foundPost.orElseThrow().title());
        assertEquals(PostStatus.PUBLISHED, foundPost.orElseThrow().status());
    }

    @Test
    void existsBySlugDelegatesToSpringDataRepository() {
        when(springDataPostRepository.existsBySlug("custom-slug")).thenReturn(true);

        boolean exists = adapter.existsBySlug("custom-slug");

        assertTrue(exists);
        verify(springDataPostRepository).existsBySlug("custom-slug");
    }

    @Test
    void findAllMapsEveryEntityToDomainObject() {
        PostEntity first = new PostEntity(
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                "First",
                "first",
                null,
                null,
                null,
                null,
                "# one",
                PostStatus.DRAFT,
                null,
                null,
                Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T09:00:00Z"),
                null,
                null,
                0L,
                0L
        );
        PostEntity second = new PostEntity(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "Second",
                "second",
                null,
                null,
                null,
                null,
                "# two",
                PostStatus.UNPUBLISHED,
                null,
                null,
                Instant.parse("2026-08-05T09:00:00Z"),
                Instant.parse("2026-08-05T10:00:00Z"),
                Instant.parse("2026-08-05T09:30:00Z"),
                null,
                0L,
                0L
        );
        when(springDataPostRepository.findAll()).thenReturn(List.of(first, second));

        List<Post> posts = adapter.findAll();

        assertEquals(2, posts.size());
        assertEquals("first", posts.get(0).slug());
        assertEquals(PostStatus.UNPUBLISHED, posts.get(1).status());
    }

    @Test
    void findByIdReturnsEmptyWhenSpringDataFindsNothing() {
        UUID postId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(springDataPostRepository.findById(postId)).thenReturn(Optional.empty());

        Optional<Post> foundPost = adapter.findById(postId);

        assertFalse(foundPost.isPresent());
    }

    private Post createPost(UUID id, String title, String slug, PostStatus status) {
        Instant createdAt = Instant.parse("2026-08-05T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-05T12:00:00Z");
        Instant publishedAt = status == PostStatus.PUBLISHED
                ? Instant.parse("2026-08-05T12:00:00Z")
                : null;
        return new Post(id, title, slug, null, null, null, null, "# content", status, null, null, createdAt, updatedAt, publishedAt, null, 0L, 0L);
    }
}
