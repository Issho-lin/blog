package com.linqibin.blog.comment.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataCommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByPostIdOrderByCreatedAtAscIdAsc(UUID postId);

    List<CommentEntity> findAllByOrderByCreatedAtDescIdAsc();

    @Transactional
    void deleteByPostId(UUID postId);
}
