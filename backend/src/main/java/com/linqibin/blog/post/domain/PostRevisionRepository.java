package com.linqibin.blog.post.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRevisionRepository {

    PostRevision save(PostRevision revision);

    Optional<PostRevision> findById(UUID id);

    List<PostRevision> findByPostIdNewestFirst(UUID postId);

    Optional<PostRevision> findLatestByPostId(UUID postId);

    void deleteById(UUID id);

    void deleteByPostId(UUID postId);
}
