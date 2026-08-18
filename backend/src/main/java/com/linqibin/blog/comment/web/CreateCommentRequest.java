package com.linqibin.blog.comment.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "称呼不能为空")
        @Size(max = 40, message = "称呼不能超过 40 个字符")
        String authorName,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 2000, message = "评论不能超过 2000 个字符")
        String content
) {
}
