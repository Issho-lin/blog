package com.linqibin.blog.ai.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiSummarizeRequest(
        @NotBlank(message = "正文不能为空")
        @Size(max = 200_000, message = "正文过长")
        String markdown
) {
}
