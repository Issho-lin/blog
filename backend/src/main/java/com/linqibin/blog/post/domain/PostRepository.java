package com.linqibin.blog.post.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(UUID id);

    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Post> findAll();
}
