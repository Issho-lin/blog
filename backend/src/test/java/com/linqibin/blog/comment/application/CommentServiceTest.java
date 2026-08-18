package com.linqibin.blog.comment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.comment.domain.Comment;
import com.linqibin.blog.comment.exception.CommentRateLimitedException;
import com.linqibin.blog.comment.infrastructure.InMemoryCommentRepository;
import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.exception.PostNotFoundException;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommentServiceTest {

    private InMemoryCommentRepository commentRepository;
    private PostService postService;
    private CommentRateLimiter rateLimiter;
    private CommentService commentService;
    private UUID publishedId;

    @BeforeEach
    void setUp() {
        InMemoryPostRepository postRepository = new InMemoryPostRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);
        postService = new PostService(postRepository, new SlugGenerator(), clock);
        Post draft = postService.createDraft("公开文章", "# 正文", "public-post", null, null);
        postService.publish(draft.id());
        publishedId = draft.id();
        postService.createDraft("草稿", "# 草稿", "draft-post", null, null);

        commentRepository = new InMemoryCommentRepository();
        rateLimiter = new CommentRateLimiter(clock, 2, 10);
        commentService = new CommentService(commentRepository, postService, rateLimiter, clock);
    }

    @Test
    void addCommentToPublishedPost() {
        Comment comment = commentService.addToPublishedPost("public-post", "读者", "写得好", "127.0.0.1");

        assertEquals(publishedId, comment.postId());
        assertEquals("读者", comment.authorName());
        assertEquals(1, commentService.listPublishedPostComments("public-post").size());
    }

    @Test
    void cannotCommentOnDraft() {
        assertThrows(PostNotFoundException.class, () ->
                commentService.addToPublishedPost("draft-post", "读者", "看不见", "127.0.0.1"));
    }

    @Test
    void rateLimitBlocksThirdCommentFromSameIp() {
        commentService.addToPublishedPost("public-post", "A", "一", "10.0.0.1");
        commentService.addToPublishedPost("public-post", "B", "二", "10.0.0.1");
        assertThrows(CommentRateLimitedException.class, () ->
                commentService.addToPublishedPost("public-post", "C", "三", "10.0.0.1"));
    }
}
