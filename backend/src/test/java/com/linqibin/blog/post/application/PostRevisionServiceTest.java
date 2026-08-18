package com.linqibin.blog.post.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionKind;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;
import com.linqibin.blog.post.infrastructure.InMemoryPostRevisionRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostRevisionServiceTest {

    private InMemoryPostRepository postRepository;
    private InMemoryPostRevisionRepository revisionRepository;
    private Clock clock;

    @BeforeEach
    void setUp() {
        postRepository = new InMemoryPostRepository();
        revisionRepository = new InMemoryPostRevisionRepository();
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    void autosaveWithinWindowCoalescesIntoOneRevision() {
        PostRevisionService revisionService = new PostRevisionService(revisionRepository, clock);
        PostService postService = new PostService(
                postRepository, new SlugGenerator(), clock, UUID::randomUUID, null, revisionService, null
        );
        Post post = postService.createDraft("标题", "# 一", "rev-post", null, null);
        postService.updatePost(post.id(), "标题", "# 二", null, null, null, post.version());
        postService.updatePost(post.id(), "标题", "# 三", null, null, null, post.version() + 1);

        assertEquals(1, revisionService.list(post.id()).size());
        assertEquals("# 三", revisionService.list(post.id()).get(0).markdownContent());
        assertEquals(PostRevisionKind.AUTO, revisionService.list(post.id()).get(0).kind());
    }

    @Test
    void publishCreatesSeparateRevision() {
        PostRevisionService revisionService = new PostRevisionService(revisionRepository, clock);
        PostService postService = new PostService(
                postRepository, new SlugGenerator(), clock, UUID::randomUUID, null, revisionService, null
        );
        Post post = postService.createDraft("标题", "# 正文", "rev-publish", null, null);
        postService.publish(post.id());

        assertTrue(revisionService.list(post.id()).stream()
                .anyMatch(revision -> revision.kind() == PostRevisionKind.PUBLISH));
    }

    @Test
    void restoreRewritesCurrentContent() {
        PostRevisionService revisionService = new PostRevisionService(revisionRepository, clock);
        PostService first = new PostService(
                postRepository, new SlugGenerator(), clock, UUID::randomUUID, null, revisionService, null
        );
        Post post = first.createDraft("旧标题", "# 旧正文", "rev-restore", null, null);
        PostRevision oldRevision = revisionService.list(post.id()).get(0);

        Clock later = Clock.fixed(Instant.parse("2026-08-18T11:00:00Z"), ZoneOffset.UTC);
        PostRevisionService laterRevisions = new PostRevisionService(revisionRepository, later);
        PostService laterService = new PostService(
                postRepository, new SlugGenerator(), later, UUID::randomUUID, null, laterRevisions, null
        );
        Post updated = laterService.updatePost(post.id(), "新标题", "# 新正文", null, null, null, post.version());
        laterService.restoreRevision(updated.id(), oldRevision.id());

        Post restored = laterService.getPost(post.id());
        assertEquals("旧标题", restored.title());
        assertEquals("# 旧正文", restored.markdownContent());
    }
}
