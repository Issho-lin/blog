package com.linqibin.blog.post.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.linqibin.blog.post.domain.DuplicateSlugException;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostNotFoundException;
import com.linqibin.blog.post.domain.PostRepository;
import com.linqibin.blog.post.domain.SlugGenerator;

public class PostService {

    private final PostRepository postRepository;
    private final SlugGenerator slugGenerator;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public PostService(PostRepository postRepository, SlugGenerator slugGenerator, Clock clock) {
        this(postRepository, slugGenerator, clock, UUID::randomUUID);
    }

    public PostService(
            PostRepository postRepository,
            SlugGenerator slugGenerator,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this.postRepository = Objects.requireNonNull(postRepository);
        this.slugGenerator = Objects.requireNonNull(slugGenerator);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public Post createDraft(String title, String markdownContent, String requestedSlug) {
        Instant now = Instant.now(clock);
        String slug = resolveSlug(title, requestedSlug);
        Post post = Post.createDraft(idSupplier.get(), title, slug, defaultContent(markdownContent), now);
        return postRepository.save(post);
    }

    public Post getPost(UUID postId) {
        return postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
    }

    public Post getPostBySlug(String slug) {
        return postRepository.findBySlug(slug).orElseThrow(() -> new PostNotFoundException(slug));
    }

    public List<Post> searchByTitleKeyword(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return postRepository.findAll().stream()
                .filter(post -> normalizedKeyword.isBlank()
                        || post.title().toLowerCase().contains(normalizedKeyword))
                .sorted(Comparator.comparing(Post::updatedAt).reversed())
                .toList();
    }

    public Post publish(UUID postId) {
        Post publishedPost = getPost(postId).publish(Instant.now(clock));
        return postRepository.save(publishedPost);
    }

    public Post unpublish(UUID postId) {
        Post unpublishedPost = getPost(postId).unpublish(Instant.now(clock));
        return postRepository.save(unpublishedPost);
    }

    public Post moveToTrash(UUID postId) {
        Post trashedPost = getPost(postId).moveToTrash(Instant.now(clock));
        return postRepository.save(trashedPost);
    }

    public Post restoreFromTrash(UUID postId) {
        Post restoredPost = getPost(postId).restoreFromTrash(Instant.now(clock));
        return postRepository.save(restoredPost);
    }

    private String resolveSlug(String title, String requestedSlug) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String normalizedRequestedSlug = slugGenerator.normalizeRequestedSlug(requestedSlug);
            if (postRepository.existsBySlug(normalizedRequestedSlug)) {
                throw new DuplicateSlugException(normalizedRequestedSlug);
            }
            return normalizedRequestedSlug;
        }

        String generatedSlug = slugGenerator.generateFromTitle(title);
        return slugGenerator.ensureUnique(generatedSlug, postRepository::existsBySlug);
    }

    private String defaultContent(String markdownContent) {
        return markdownContent == null ? "" : markdownContent;
    }
}
