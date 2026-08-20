package com.linqibin.blog.ai.web;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.ai.application.AiChatRateLimiter;
import com.linqibin.blog.ai.application.AiSettingsService;
import com.linqibin.blog.ai.exception.AgentUnavailableException;
import com.linqibin.blog.ai.infrastructure.AgentClient;

@RestController
@RequestMapping("/api/public/ai")
public class PublicAiController {

    private final AiSettingsService aiSettingsService;
    private final AgentClient agentClient;
    private final AiChatRateLimiter aiChatRateLimiter;

    public PublicAiController(
            AiSettingsService aiSettingsService,
            AgentClient agentClient,
            AiChatRateLimiter aiChatRateLimiter
    ) {
        this.aiSettingsService = aiSettingsService;
        this.agentClient = agentClient;
        this.aiChatRateLimiter = aiChatRateLimiter;
    }

    @GetMapping("/status")
    public PublicAiStatusResponse status() {
        return new PublicAiStatusResponse(aiSettingsService.get().publicAssistantAvailable());
    }

    @PostMapping("/chat")
    public PublicAiChatResponse chat(
            @Valid @RequestBody PublicAiChatRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!aiSettingsService.get().publicAssistantAvailable()) {
            throw new AgentUnavailableException("公开助手未开启");
        }
        aiChatRateLimiter.assertAllowed(clientIp(httpRequest));
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId().trim();
        List<PublicAiChatRequest.PublicAiChatMessage> messages =
                request.messages() == null ? List.of() : request.messages();
        return agentClient.chat(sessionId, messages);
    }

    @PostMapping("/chat/stream")
    public void chatStream(
            @Valid @RequestBody PublicAiChatRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws IOException {
        if (!aiSettingsService.get().publicAssistantAvailable()) {
            throw new AgentUnavailableException("公开助手未开启");
        }
        aiChatRateLimiter.assertAllowed(clientIp(httpRequest));
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId().trim();
        List<PublicAiChatRequest.PublicAiChatMessage> messages =
                request.messages() == null ? List.of() : request.messages();
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        httpResponse.setContentType("text/event-stream;charset=UTF-8");
        httpResponse.setHeader("Cache-Control", "no-cache, no-transform");
        httpResponse.setHeader("Connection", "keep-alive");
        httpResponse.setHeader("X-Accel-Buffering", "no");
        httpResponse.setBufferSize(256);
        httpResponse.flushBuffer();
        agentClient.streamChat(sessionId, messages, httpResponse.getOutputStream());
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
