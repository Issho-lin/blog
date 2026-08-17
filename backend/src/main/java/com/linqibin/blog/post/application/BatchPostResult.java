package com.linqibin.blog.post.application;

import java.util.List;
import java.util.UUID;

import com.linqibin.blog.post.domain.Post;

// 批量操作结果：部分成功时仍然返回，失败项带上原因方便前端提示。
public record BatchPostResult(
        List<Post> succeeded,
        List<Failure> failed
) {

    public record Failure(UUID id, String message) {
    }
}
