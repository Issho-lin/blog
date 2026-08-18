package com.linqibin.blog.post.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionRepository;

public class InMemoryPostRevisionRepository implements PostRevisionRepository {

    private final ConcurrentHashMap<UUID, PostRevision> revisions = new ConcurrentHashMap<>();

    public void clear() {
        revisions.clear();
    }

    @Override
    public PostRevision save(PostRevision revision) {
        revisions.put(revision.id(), revision);
        return revision;
    }

    @Override
    public Optional<PostRevision> findById(UUID id) {
        return Optional.ofNullable(revisions.get(id));
    }

    @Override
    public List<PostRevision> findByPostIdNewestFirst(UUID postId) {
        return revisions.values().stream()
                .filter(revision -> revision.postId().equals(postId))
                .sorted(Comparator.comparing(PostRevision::createdAt).reversed().thenComparing(PostRevision::id))
                .toList();
    }

    @Override
    public Optional<PostRevision> findLatestByPostId(UUID postId) {
        return findByPostIdNewestFirst(postId).stream().findFirst();
    }

    @Override
    public void deleteById(UUID id) {
        revisions.remove(id);
    }

    @Override
    public void deleteByPostId(UUID postId) {
        revisions.values().removeIf(revision -> revision.postId().equals(postId));
    }
}
