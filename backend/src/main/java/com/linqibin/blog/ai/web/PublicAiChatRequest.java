package com.linqibin.blog.ai.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicAiChatRequest(
        String sessionId,
        @Valid
        @NotNull
        @Size(min = 1, max = 20, message = "对话轮次过多")
        List<PublicAiChatMessage> messages
) {

    public record PublicAiChatMessage(
            @NotBlank String role,
            @NotBlank @Size(max = 4000, message = "单条消息过长") String content
    ) {
    }
}
