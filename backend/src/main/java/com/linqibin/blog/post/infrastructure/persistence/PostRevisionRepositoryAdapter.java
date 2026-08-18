package com.linqibin.blog.post.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionRepository;

public class PostRevisionRepositoryAdapter implements PostRevisionRepository {

    private final SpringDataPostRevisionRepository springDataPostRevisionRepository;
    private final PostRevisionEntityMapper postRevisionEntityMapper;

    public PostRevisionRepositoryAdapter(
            SpringDataPostRevisionRepository springDataPostRevisionRepository,
            PostRevisionEntityMapper postRevisionEntityMapper
    ) {
        this.springDataPostRevisionRepository = springDataPostRevisionRepository;
        this.postRevisionEntityMapper = postRevisionEntityMapper;
    }

    @Override
    public PostRevision save(PostRevision revision) {
        PostRevisionEntity saved = springDataPostRevisionRepository.save(postRevisionEntityMapper.toEntity(revision));
        return postRevisionEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<PostRevision> findById(UUID id) {
        return springDataPostRevisionRepository.findById(id).map(postRevisionEntityMapper::toDomain);
    }

    @Override
    public List<PostRevision> findByPostIdNewestFirst(UUID postId) {
        return springDataPostRevisionRepository.findByPostIdOrderByCreatedAtDescIdAsc(postId).stream()
                .map(postRevisionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PostRevision> findLatestByPostId(UUID postId) {
        return springDataPostRevisionRepository.findFirstByPostIdOrderByCreatedAtDescIdAsc(postId)
                .map(postRevisionEntityMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        springDataPostRevisionRepository.deleteById(id);
    }

    @Override
    public void deleteByPostId(UUID postId) {
        springDataPostRevisionRepository.deleteByPostId(postId);
    }
}
