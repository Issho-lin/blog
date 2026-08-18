package com.linqibin.blog.comment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.linqibin.blog.comment.domain.Comment;
import com.linqibin.blog.comment.domain.CommentRepository;
import com.linqibin.blog.comment.exception.CommentNotFoundException;
import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;

public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final CommentRateLimiter rateLimiter;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public CommentService(
            CommentRepository commentRepository,
            PostService postService,
            CommentRateLimiter rateLimiter,
            Clock clock
    ) {
        this(commentRepository, postService, rateLimiter, clock, UUID::randomUUID);
    }

    public CommentService(
            CommentRepository commentRepository,
            PostService postService,
            CommentRateLimiter rateLimiter,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this.commentRepository = Objects.requireNonNull(commentRepository);
        this.postService = Objects.requireNonNull(postService);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public List<Comment> listPublishedPostComments(String slug) {
        Post post = postService.getPublishedPostBySlug(slug);
        return commentRepository.findByPostId(post.id());
    }

    public Comment addToPublishedPost(String slug, String authorName, String content, String ip) {
        Post post = postService.getPublishedPostBySlug(slug);
        rateLimiter.assertAllowed(ip == null ? slug : ip);
        Instant now = Instant.now(clock);
        Comment comment = Comment.create(idSupplier.get(), post.id(), authorName, content, ip, now);
        return commentRepository.save(comment);
    }

    public List<Comment> listAllForAdmin() {
        return commentRepository.findAllNewestFirst();
    }

    public void delete(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        commentRepository.deleteById(comment.id());
    }

    public void deleteByPostId(UUID postId) {
        commentRepository.deleteByPostId(postId);
    }
}
