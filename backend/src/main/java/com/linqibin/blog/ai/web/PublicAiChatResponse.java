package com.linqibin.blog.ai.web;

import java.util.List;

public record PublicAiChatResponse(
        String sessionId,
        String text,
        List<PublicAiCitation> citations
) {
}
