package com.linqibin.blog.post.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRepository;

public class InMemoryPostRepository implements PostRepository {

    private final ConcurrentHashMap<UUID, Post> posts = new ConcurrentHashMap<>();

    public void clear() {
        posts.clear();
    }

    @Override
    public Post save(Post post) {
        posts.put(post.id(), post);
        return post;
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return Optional.ofNullable(posts.get(id));
    }

    @Override
    public Optional<Post> findBySlug(String slug) {
        return posts.values().stream()
                .filter(post -> post.slug().equals(slug))
                .findFirst();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return posts.values().stream().anyMatch(post -> post.slug().equals(slug));
    }

    @Override
    public List<Post> findAll() {
        return List.copyOf(posts.values());
    }
}
