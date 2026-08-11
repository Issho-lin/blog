package com.linqibin.blog.post.exception;

import java.util.UUID;

import com.linqibin.blog.post.domain.Post;

// 并发修改异常：当客户端提交的版本号与服务端当前版本号不匹配时抛出。
// 通常发生在两个窗口同时编辑同一篇文章的场景：后提交的一方版本过期，服务端拒绝覆盖。
// 携带服务端当前文章数据，让前端可以对比后决定覆盖、保留本地内容或放弃修改。
public class ConcurrentPostModificationException extends RuntimeException {

    private final UUID postId;
    private final long expectedVersion;
    private final long actualVersion;
    private final Post currentPost;

    public ConcurrentPostModificationException(UUID postId, long expectedVersion, long actualVersion) {
        this(postId, expectedVersion, actualVersion, null);
    }

    public ConcurrentPostModificationException(UUID postId, long expectedVersion, long actualVersion, Post currentPost) {
        super("文章已被其他人修改，请刷新后重试。postId=" + postId
                + ", 期望版本=" + expectedVersion + ", 当前版本=" + actualVersion);
        this.postId = postId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
        this.currentPost = currentPost;
    }

    public UUID getPostId() {
        return postId;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }

    public long getActualVersion() {
        return actualVersion;
    }

    public Post getCurrentPost() {
        return currentPost;
    }
}
