package com.linqibin.blog.comment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    List<Comment> findByPostId(UUID postId);

    List<Comment> findAllNewestFirst();

    void deleteById(UUID id);

    void deleteByPostId(UUID postId);
}
