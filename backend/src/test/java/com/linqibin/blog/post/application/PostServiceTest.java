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

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.exception.ConcurrentPostModificationException;
import com.linqibin.blog.post.exception.DuplicateSlugException;
import com.linqibin.blog.post.exception.InvalidPostStateTransitionException;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Post post = postService.createDraft(" Hello, Spring Boot! ", "# content", null, null, null);

        assertEquals("Hello, Spring Boot!", post.title());
        assertEquals("hello-spring-boot", post.slug());
        assertEquals(PostStatus.DRAFT, post.status());
    }

    @Test
    void createDraftWithChineseTitleFallsBackToShortIdSlug() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        Post post = postService.createDraft("你好，个人博客", "# content", null, null, null);

        assertEquals("post-fallback-1", post.slug());
    }

    @Test
    void createDraftWithDuplicateGeneratedSlugAppendsNumericSuffix() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        Post firstPost = postService.createDraft("Hello World", "# content", null, null, null);
        Post secondPost = postService.createDraft("Hello World", "# content", null, null, null);

        assertEquals("hello-world", firstPost.slug());
        assertEquals("hello-world-2", secondPost.slug());
    }

    @Test
    void createDraftWithDuplicateRequestedSlugThrowsException() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");

        postService.createDraft("First", "# content", "custom-slug", null, null);

        assertThrows(
                DuplicateSlugException.class,
                () -> postService.createDraft("Second", "# content", "custom-slug", null, null)
        );
    }

    @Test
    void getPostBySlugReturnsSavedPost() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");
        Post createdPost = postService.createDraft("My Post", "# content", null, null, null);

        Post foundPost = postService.getPostBySlug("my-post");

        assertEquals(createdPost.id(), foundPost.id());
    }

    @Test
    void searchByTitleKeywordIgnoresCase() {
        PostService postService = createServiceAt("2026-07-30T10:00:00Z");
        Post springPost = postService.createDraft("Spring Boot Guide", "# content", null, null, null);
        postService.createDraft("Redis Notes", "# content", null, null, null);

        assertIterableEquals(
                java.util.List.of(springPost),
                postService.searchByTitleKeyword("spring")
        );
    }

    @Test
    void updateDraftChangesTitleContentAndKeepsSlugWhenNotProvided() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Original Title", "# old", null, null, null);
        PostService updateService = createServiceAt("2026-07-30T11:00:00Z");

        Post updatedPost = updateService.updatePost(draftPost.id(), "Updated Title", "# new", null, null, null, null);

        assertEquals("Updated Title", updatedPost.title());
        assertEquals("# new", updatedPost.markdownContent());
        assertEquals("original-title", updatedPost.slug());
        assertEquals(PostStatus.DRAFT, updatedPost.status());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), updatedPost.updatedAt());
    }

    @Test
    void updateDraftWithBlankSlugRegeneratesSlugFromLatestTitle() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Original Title", "# old", null, null, null);
        PostService updateService = createServiceAt("2026-07-30T11:00:00Z");

        Post updatedPost = updateService.updatePost(draftPost.id(), "New Url Title", "# old", "", null, null, null);

        assertEquals("new-url-title", updatedPost.slug());
    }

    @Test
    void updatePostWithDuplicateRequestedSlugThrowsException() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        draftService.createDraft("First Post", "# first", "custom-slug", null, null);
        Post secondPost = draftService.createDraft("Second Post", "# second", null, null, null);
        PostService updateService = createServiceAt("2026-07-30T11:00:00Z");

        assertThrows(
                DuplicateSlugException.class,
                () -> updateService.updatePost(secondPost.id(), "Second Post", "# second", "custom-slug", null, null, null)
        );
    }

    @Test
    void updateTrashedPostThrowsInvalidStateTransitionException() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Trash Me", "# content", null, null, null);
        PostService trashService = createServiceAt("2026-07-30T11:00:00Z");
        trashService.moveToTrash(draftPost.id());
        PostService updateService = createServiceAt("2026-07-30T12:00:00Z");

        assertThrows(
                InvalidPostStateTransitionException.class,
                () -> updateService.updatePost(draftPost.id(), "Updated", "# content", null, null, null, null)
        );
    }

    @Test
    void updatePublishedPostWithBlankContentThrowsIllegalArgumentException() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Published Post", "# content", null, null, null);
        PostService publishService = createServiceAt("2026-07-30T11:00:00Z");
        publishService.publish(draftPost.id());
        PostService updateService = createServiceAt("2026-07-30T12:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> updateService.updatePost(draftPost.id(), "Published Post", "", null, null, null, null)
        );
    }

    @Test
    void publishDraftChangesStatusAndSetsPublishedAt() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Publish Me", "# content", null, null, null);
        PostService publishService = createServiceAt("2026-07-30T11:00:00Z");

        Post publishedPost = publishService.publish(draftPost.id());

        assertEquals(PostStatus.PUBLISHED, publishedPost.status());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), publishedPost.publishedAt());
        assertEquals(Instant.parse("2026-07-30T11:00:00Z"), publishedPost.updatedAt());
    }

    @Test
    void republishKeepsFirstPublishedAt() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Publish Twice", "# content", null, null, null);
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
        Post draftPost = draftService.createDraft("Unpublish Me", "# content", null, null, null);
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
        Post draftPost = draftService.createDraft("Trash Me", "# content", null, null, null);
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
    void permanentlyDeleteRemovesTrashedPostAndFreesSlug() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Delete Me", "# content", "delete-me", null, null);
        PostService trashService = createServiceAt("2026-07-30T11:00:00Z");
        trashService.moveToTrash(draftPost.id());
        PostService deleteService = createServiceAt("2026-07-30T12:00:00Z");

        deleteService.permanentlyDelete(draftPost.id());

        assertTrue(postRepository.findById(draftPost.id()).isEmpty());
        assertFalse(postRepository.existsBySlug("delete-me"));

        Post recreated = deleteService.createDraft("Delete Me", "# content", "delete-me", null, null);
        assertEquals("delete-me", recreated.slug());
    }

    @Test
    void permanentlyDeleteDraftThrowsInvalidStateTransitionException() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Keep Draft", "# content", null, null, null);
        PostService deleteService = createServiceAt("2026-07-30T11:00:00Z");

        assertThrows(
                InvalidPostStateTransitionException.class,
                () -> deleteService.permanentlyDelete(draftPost.id())
        );
        assertTrue(postRepository.findById(draftPost.id()).isPresent());
    }

    @Test
    void publishTrashedPostThrowsInvalidStateTransitionException() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Invalid Publish", "# content", null, null, null);
        PostService trashService = createServiceAt("2026-07-30T11:00:00Z");
        trashService.moveToTrash(draftPost.id());
        PostService publishService = createServiceAt("2026-07-30T12:00:00Z");

        assertThrows(
                InvalidPostStateTransitionException.class,
                () -> publishService.publish(draftPost.id())
        );
    }

    @Test
    void updatePostWithUnchangedContentDoesNotIncrementVersion() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Same Title", "# content", null, null, null);
        PostService updateService = createServiceAt("2026-07-30T11:00:00Z");

        // 用相同标题、相同正文、不传 slug（保持原值）再次保存。
        Post result = updateService.updatePost(draftPost.id(), "Same Title", "# content", null, null, null, null);

        // 内容未变时不递增版本号，避免自动保存产生虚假冲突。
        assertEquals(0L, result.version());
        // 更新时间也保持不变，因为没有实际修改。
        assertEquals(draftPost.updatedAt(), result.updatedAt());
    }

    @Test
    void updatePostWithChangedContentIncrementsVersion() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Original", "# old", null, null, null);
        PostService updateService = createServiceAt("2026-07-30T11:00:00Z");

        Post result = updateService.updatePost(draftPost.id(), "Updated", "# new", null, null, null, null);

        assertEquals(1L, result.version());
    }

    @Test
    void updatePostWithChangedTitleBypassesStaleVersionCheck() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Title", "# content", null, null, null);
        PostService firstUpdateService = createServiceAt("2026-07-30T11:00:00Z");
        firstUpdateService.updatePost(draftPost.id(), "Title V2", "# content", null, null, null, 0L);
        // version 现在是 1

        // 用过期的 expectedVersion=0 但内容与当前服务端一致，不应抛冲突。
        PostService secondUpdateService = createServiceAt("2026-07-30T12:00:00Z");
        Post result = secondUpdateService.updatePost(draftPost.id(), "Title V2", "# content", null, null, null, 0L);

        // 内容未变，直接返回当前文章，version 仍为 1。
        assertEquals(1L, result.version());
    }

    @Test
    void updatePostWithChangedContentAndStaleVersionThrowsConflict() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Concurrent", "# old", null, null, null);
        PostService firstUpdateService = createServiceAt("2026-07-30T11:00:00Z");
        firstUpdateService.updatePost(draftPost.id(), "Concurrent V2", "# updated", null, null, null, 0L);
        // version 现在是 1

        // 用过期的 expectedVersion=0 且内容不同，应抛冲突。
        PostService staleUpdateService = createServiceAt("2026-07-30T12:00:00Z");
        assertThrows(
                ConcurrentPostModificationException.class,
                () -> staleUpdateService.updatePost(draftPost.id(), "Stale", "# stale", null, null, null, 0L)
        );
    }

    @Test
    void getSaveStatusReturnsCurrentPost() {
        PostService draftService = createServiceAt("2026-07-30T10:00:00Z");
        Post draftPost = draftService.createDraft("Status Check", "# content", null, null, null);

        Post saveStatus = draftService.getSaveStatus(draftPost.id());

        assertEquals(draftPost.version(), saveStatus.version());
        assertEquals(draftPost.updatedAt(), saveStatus.updatedAt());
        assertEquals(draftPost.status(), saveStatus.status());
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
