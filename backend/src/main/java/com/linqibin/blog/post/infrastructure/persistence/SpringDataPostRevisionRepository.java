package com.linqibin.blog.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataPostRevisionRepository extends JpaRepository<PostRevisionEntity, UUID> {

    List<PostRevisionEntity> findByPostIdOrderByCreatedAtDescIdAsc(UUID postId);

    Optional<PostRevisionEntity> findFirstByPostIdOrderByCreatedAtDescIdAsc(UUID postId);

    @Transactional
    void deleteByPostId(UUID postId);
}
