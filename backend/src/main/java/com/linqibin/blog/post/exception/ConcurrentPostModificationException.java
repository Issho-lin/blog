package com.linqibin.blog.post.exception;

import java.util.UUID;

// 并发修改异常：当客户端提交的版本号与服务端当前版本号不匹配时抛出。
// 通常发生在两个窗口同时编辑同一篇文章的场景：后提交的一方版本过期，服务端拒绝覆盖。
public class ConcurrentPostModificationException extends RuntimeException {

    private final UUID postId;
    private final long expectedVersion;
    private final long actualVersion;

    public ConcurrentPostModificationException(UUID postId, long expectedVersion, long actualVersion) {
        super("文章已被其他人修改，请刷新后重试。postId=" + postId
                + ", 期望版本=" + expectedVersion + ", 当前版本=" + actualVersion);
        this.postId = postId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
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
}
