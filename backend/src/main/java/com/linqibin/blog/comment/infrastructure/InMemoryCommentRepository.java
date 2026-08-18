package com.linqibin.blog.comment.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.comment.domain.Comment;
import com.linqibin.blog.comment.domain.CommentRepository;

public class InMemoryCommentRepository implements CommentRepository {

    private final ConcurrentHashMap<UUID, Comment> comments = new ConcurrentHashMap<>();

    public void clear() {
        comments.clear();
    }

    @Override
    public Comment save(Comment comment) {
        comments.put(comment.id(), comment);
        return comment;
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return Optional.ofNullable(comments.get(id));
    }

    @Override
    public List<Comment> findByPostId(UUID postId) {
        return comments.values().stream()
                .filter(comment -> comment.postId().equals(postId))
                .sorted(Comparator.comparing(Comment::createdAt).thenComparing(Comment::id))
                .toList();
    }

    @Override
    public List<Comment> findAllNewestFirst() {
        return comments.values().stream()
                .sorted(Comparator.comparing(Comment::createdAt).reversed().thenComparing(Comment::id))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        comments.remove(id);
    }

    @Override
    public void deleteByPostId(UUID postId) {
        comments.values().removeIf(comment -> comment.postId().equals(postId));
    }
}
