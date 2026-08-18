package com.linqibin.blog.comment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.comment.domain.Comment;
import com.linqibin.blog.comment.domain.CommentRepository;

public class CommentRepositoryAdapter implements CommentRepository {

    private final SpringDataCommentRepository springDataCommentRepository;
    private final CommentEntityMapper commentEntityMapper;

    public CommentRepositoryAdapter(
            SpringDataCommentRepository springDataCommentRepository,
            CommentEntityMapper commentEntityMapper
    ) {
        this.springDataCommentRepository = springDataCommentRepository;
        this.commentEntityMapper = commentEntityMapper;
    }

    @Override
    public Comment save(Comment comment) {
        CommentEntity saved = springDataCommentRepository.save(commentEntityMapper.toEntity(comment));
        return commentEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return springDataCommentRepository.findById(id).map(commentEntityMapper::toDomain);
    }

    @Override
    public List<Comment> findByPostId(UUID postId) {
        return springDataCommentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId).stream()
                .map(commentEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Comment> findAllNewestFirst() {
        return springDataCommentRepository.findAllByOrderByCreatedAtDescIdAsc().stream()
                .map(commentEntityMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataCommentRepository.deleteById(id);
    }

    @Override
    public void deleteByPostId(UUID postId) {
        springDataCommentRepository.deleteByPostId(postId);
    }
}
