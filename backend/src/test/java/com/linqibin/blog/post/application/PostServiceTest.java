package com.linqibin.blog.post.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.post.domain.DuplicateSlugException;
import com.linqibin.blog.post.domain.InvalidPostStateTransitionException;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostServiceTest {

    private InMemoryPostRepository postRepository;
    private SlugGenerator slugGenerator;
    private Supplier<UUID> idSupplier;
    private AtomicInteger postIdCounter;
    private AtomicInteger fallbackSlugCounter;

    @BeforeEach
    void setUp() {
        this.postRepository = new InMemoryPostRepository();
        this.postIdCounter = new AtomicInteger(1);
        this.fallbackSlugCounter = new AtomicInteger(1);
        this.idSupplier = () -> UUID.nameUUIDFromBytes(
                ("post-" + postIdCounter.getAndIncrement()).getBytes(StandardCharsets.UTF_8)
        );
        this.slugGenerator = new SlugGenerator(() -> "fallback-" + fallbackSlugCounter.getAndIncrement());
    }

    @Test
    void createDraftGeneratesNormalizedSlugFromEnglishTitle() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        Post post = postService.createDraft(" Hello, Spring Boot! ", "# content", null);

        assertEquals("Hello, Spring Boot!", post.title());
        assertEquals("hello-spring-boot", post.slug());
        assertEquals(PostStatus.DRAFT, post.status());
    }

    @Test
    void createDraftWithChineseTitleFallsBackToShortIdSlug() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        Post post = postService.createDraft("你好，个人博客", "# content", null);

        assertEquals("post-fallback-1", post.slug());
    }

    @Test
    void createDraftWithDuplicateGeneratedSlugAppendsNumericSuffix() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        Post firstPost = postService.createDraft("Hello World", "# content", null);
        Post secondPost = postService.createDraft("Hello World", "# content", null);

        assertEquals("hello-world", firstPost.slug());
        assertEquals("hello-world-2", secondPost.slug());
    }

    @Test
    void createDraftWithDuplicateRequestedSlugThrowsException() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        postService.createDraft("First", "# content", "custom-slug");

        assertThrows(
                DuplicateSlugException.class,
                () -> postService.createDraft("Second", "# content", "custom-slug")
        );
    }

    @Test
    void getPostBySlugReturnsSavedPost() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");
        Post createdPost = postService.createDraft("My Post", "# content", null);

        Post foundPost = postService.getPostBySlug("my-post");

        assertEquals(createdPost.id(), foundPost.id());
    }

    @Test
    void searchByTitleKeywordIgnoresCase() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");
        Post springPost = postService.createDraft("Spring Boot Guide", "# content", null);
        postService.createDraft("Redis Notes", "# content", null);

        assertIterableEquals(
                java.util.List.of(springPost),
                postService.searchByTitleKeyword("spring")
        );
    }

    @Test
    void publishDraftChangesStatusAndSetsPublishedAt() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Publish Me", "# content", null);
        PostService publishService = createServiceAt("2026-07-30T11:00:00Z");

        Post publishedPost = publishService.publish(draftPost.id());

        assertEquals(PostStatus.PUBLISHED, publishedPost.status());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), publishedPost.publishedAt());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), publishedPost.updatedAt());
    }

    @Test
    void republishKeepsFirstPublishedAt() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Publish Twice", "# content", null);
        PostService firstPublishService = createServiceAt("2026-07-30T11:00:00Z");
        firstPublishService.publish(draftPost.id());
        PostService republishService = createServiceAt("2026-07-30T12:00:00Z");

        Post republishedPost = republishService.publish(draftPost.id());

        assertEquals(PostStatus.PUBLISHED, republishedPost.status());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), republishedPost.publishedAt());
        assertEquals(Instant.parse("2026-07-30T12:00:00Z"), republishedPost.updatedAt());
    }

    @Test
    void unpublishPublishedPostChangesStatus() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Unpublish Me", "# content", null);
        PostService publishService = createServiceAt("2026-07-30T11:00:00Z");
        publishService.publish(draftPost.id());
        PostService unpublishService = createServiceAt("2026-07-30T12:00:00Z");

        Post unpublishedPost = unpublishService.unpublish(draftPost.id());

        assertEquals(PostStatus.UNPUBLISHED, unpublishedPost.status());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), unpublishedPost.publishedAt());
        assertEquals(Instant.parse("2026-07-30T12:00:00Z"), unpublishedPost.updatedAt());
    }

    @Test
    void moveToTrashAndRestoreReturnsPreviousUnpublishedState() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Trash Me", "# content", null);
        PostService publishService = createServiceAt("2026-07-30T11:00:00Z");
        publishService.publish(draftPost.id());
        PostService unpublishService = createServiceAt("2026-07-30T12:00:00Z");
        unpublishService.unpublish(draftPost.id());
        PostService trashService = createServiceAt("2026-07-30T13:00:00Z");
        Post trashedPost = trashService.moveToTrash(draftPost.id());
        PostService restoreService = createServiceAt("2026-07-30T14:00:00Z");

        Post restoredPost = restoreService.restoreFromTrash(draftPost.id());

        assertEquals(PostStatus.TRASHED, trashedPost.status());
        assertEquals(PostStatus.UNPUBLISHED, restoredPost.status());
        assertEquals(Instant.parse("2026-07-30T14:00:00Z"), restoredPost.updatedAt());
    }

    @Test
    void publishTrashedPostThrowsInvalidStateTransitionException() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Invalid Publish", "# content", null);
        PostService trashService = createServiceAt("2026-07-30T11:00:00Z");
        trashService.moveToTrash(draftPost.id());
        PostService publishService = createServiceAt("2026-07-30T12:00:00Z");

        assertThrows(
                InvalidPostStateTransitionException.class,
                () -> publishService.publish(draftPost.id())
        );
    }

    private PostService createServiceAt(String instantValue) {
        return new PostService(
                postRepository,
                slugGenerator,
                Clock.fixed(Instant.parse(instantValue), ZoneOffset.UTC),
                idSupplier
        );
    }
}
