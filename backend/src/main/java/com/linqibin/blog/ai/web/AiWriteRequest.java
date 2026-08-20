package com.linqibin.blog.ai.web;

import jakarta.validation.constraints.Size;

public record AiWriteRequest(
        String mode,
        @Size(max = 4000, message = "指令过长")
        String instruction,
        @Size(max = 200_000, message = "正文过长")
        String markdown
) {
}
