package com.linqibin.blog.post.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionKind;
import com.linqibin.blog.post.domain.PostRevisionRepository;
import com.linqibin.blog.post.exception.PostRevisionNotFoundException;

public class PostRevisionService {

    static final Duration AUTO_COALESCE_WINDOW = Duration.ofMinutes(10);

    private final PostRevisionRepository revisionRepository;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;

    public PostRevisionService(PostRevisionRepository revisionRepository, Clock clock) {
        this(revisionRepository, clock, UUID::randomUUID);
    }

    public PostRevisionService(
            PostRevisionRepository revisionRepository,
            Clock clock,
            Supplier<UUID> idSupplier
    ) {
        this.revisionRepository = Objects.requireNonNull(revisionRepository);
        this.clock = Objects.requireNonNull(clock);
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public void record(Post post, PostRevisionKind kind) {
        Instant now = Instant.now(clock);
        PostRevision latest = revisionRepository.findLatestByPostId(post.id()).orElse(null);
        if (latest != null && latest.sameContent(post) && latest.kind() == kind) {
            return;
        }
        if (kind == PostRevisionKind.AUTO && latest != null && latest.kind() == PostRevisionKind.AUTO
                && !latest.createdAt().plus(AUTO_COALESCE_WINDOW).isBefore(now)) {
            revisionRepository.deleteById(latest.id());
        }
        revisionRepository.save(PostRevision.snapshot(idSupplier.get(), post, kind, now));
    }

    public List<PostRevision> list(UUID postId) {
        return revisionRepository.findByPostIdNewestFirst(postId);
    }

    public PostRevision get(UUID postId, UUID revisionId) {
        PostRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new PostRevisionNotFoundException(revisionId));
        if (!revision.postId().equals(postId)) {
            throw new PostRevisionNotFoundException(revisionId);
        }
        return revision;
    }

    public void deleteByPostId(UUID postId) {
        revisionRepository.deleteByPostId(postId);
    }
}
