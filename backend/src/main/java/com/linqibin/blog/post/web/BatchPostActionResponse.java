package com.linqibin.blog.post.web;

import java.util.List;
import java.util.UUID;

import com.linqibin.blog.post.application.BatchPostResult;

// 管理端批量操作响应：成功的文章和失败原因分开返回。
public record BatchPostActionResponse(
        List<PostResponse> succeeded,
        List<Failure> failed
) {

    public record Failure(UUID id, String message) {
    }

    public static BatchPostActionResponse from(BatchPostResult result) {
        return new BatchPostActionResponse(
                result.succeeded().stream().map(PostResponse::from).toList(),
                result.failed().stream()
                        .map(item -> new Failure(item.id(), item.message()))
                        .toList()
        );
    }
}
