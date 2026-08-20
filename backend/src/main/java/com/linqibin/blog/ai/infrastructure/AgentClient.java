package com.linqibin.blog.ai.infrastructure;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import com.linqibin.blog.ai.application.AiSettingsService;
import com.linqibin.blog.ai.domain.AiSettings;
import com.linqibin.blog.ai.exception.AgentUnavailableException;
import com.linqibin.blog.ai.web.PublicAiChatRequest;
import com.linqibin.blog.ai.web.PublicAiChatResponse;
import com.linqibin.blog.ai.web.PublicAiCitation;

@Component
public class AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String projectId;
    private final AiSettingsService aiSettingsService;

    public AgentClient(
            @Value("${blog.ai.base-url:http://localhost:8090}") String baseUrl,
            @Value("${blog.ai.api-key:dev-agent-key}") String apiKey,
            @Value("${blog.ai.project-id:blog}") String projectId,
            AiSettingsService aiSettingsService,
            JsonMapper jsonMapper
    ) {
        this.apiKey = apiKey;
        this.projectId = projectId;
        this.aiSettingsService = aiSettingsService;
        this.jsonMapper = jsonMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(this.httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public String projectId() {
        return projectId;
    }

    public String complete(String scenario, String text, String instruction, String mode, String context) {
        ensureEnabled();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scenario", scenario);
        payload.put("text", text == null ? "" : text);
        payload.put("instruction", instruction == null ? "" : instruction);
        payload.put("mode", mode);
        payload.put("context", context == null ? "" : context);
        putLlm(payload);
        return extractText(postJson("/v1/complete", payload));
    }

    public PublicAiChatResponse chat(String sessionId, List<PublicAiChatRequest.PublicAiChatMessage> messages) {
        Map<?, ?> response = postJson("/v1/chat", chatPayload(sessionId, messages, false));
        return new PublicAiChatResponse(
                sessionId,
                extractText(response),
                extractCitations(response)
        );
    }

    public void streamChat(
            String sessionId,
            List<PublicAiChatRequest.PublicAiChatMessage> messages,
            OutputStream outputStream
    ) {
        ensureEnabled();
        Map<String, Object> payload = chatPayload(sessionId, messages, true);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("X-API-Key", apiKey)
                    .header("Accept", MediaType.TEXT_EVENT_STREAM_VALUE)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(jsonMapper.writeValueAsBytes(payload)))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new AgentUnavailableException("AI 服务调用失败: " + response.statusCode());
            }
            try (InputStream body = response.body()) {
                if (body == null) {
                    throw new AgentUnavailableException("AI 服务返回空响应");
                }
                byte[] buffer = new byte[256];
                int read;
                while ((read = body.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    outputStream.flush();
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AgentUnavailableException("无法连接 AI 服务", exception);
        } catch (IOException exception) {
            throw new AgentUnavailableException("无法连接 AI 服务", exception);
        }
    }

    private Map<String, Object> chatPayload(
            String sessionId,
            List<PublicAiChatRequest.PublicAiChatMessage> messages,
            boolean stream
    ) {
        ensureEnabled();
        AiSettings settings = aiSettingsService.get();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session_id", sessionId);
        payload.put("project_id", projectId);
        payload.put("messages", messages == null ? List.of() : messages.stream()
                .map(item -> Map.of("role", item.role(), "content", item.content()))
                .toList());
        payload.put("rag", Map.of("enabled", true, "corpus", "published", "top_k", 6));
        payload.put("system_prompt", settings.assistantPersona());
        payload.put("stream", stream);
        putLlm(payload);
        putEmbed(payload);
        return payload;
    }

    public void upsertDocument(UUID docId, String corpus, String title, String text, Map<String, Object> metadata) {
        if (!aiSettingsService.get().enabled()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("corpus", corpus);
        payload.put("title", title);
        payload.put("text", text);
        payload.put("content_type", "markdown");
        payload.put("metadata", metadata == null ? Map.of() : metadata);
        putEmbed(payload);
        try {
            restClient.put()
                    .uri("/v1/projects/{projectId}/documents/{docId}", projectId, docId)
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("同步文档到 AI 服务失败: docId={}", docId, exception);
        }
    }

    public void deleteDocument(UUID docId) {
        if (!aiSettingsService.get().enabled()) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/v1/projects/{projectId}/documents/{docId}", projectId, docId)
                    .header("X-API-Key", apiKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("从 AI 服务删除文档失败: docId={}", docId, exception);
        }
    }

    private void putEmbed(Map<String, Object> payload) {
        AiSettings settings = aiSettingsService.get();
        if (!settings.hasEmbedCredentials()) {
            return;
        }
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("base_url", settings.resolvedEmbedBaseUrl());
        embed.put("api_key", settings.resolvedEmbedApiKey());
        embed.put("model", settings.embedModel());
        embed.put("dimensions", settings.embedDimensions());
        payload.put("embed", embed);
    }

    private void putLlm(Map<String, Object> payload) {
        AiSettings settings = aiSettingsService.get();
        if (!settings.hasChatCredentials()) {
            return;
        }
        Map<String, Object> llm = new LinkedHashMap<>();
        llm.put("base_url", settings.chatBaseUrl());
        llm.put("api_key", settings.chatApiKey());
        llm.put("model", settings.chatModel());
        payload.put("llm", llm);
    }

    private void ensureEnabled() {
        if (!aiSettingsService.get().enabled()) {
            throw new AgentUnavailableException("AI 服务未启用，请在后台设置中打开并填写模型");
        }
    }

    @SuppressWarnings("rawtypes")
    private Map<?, ?> postJson(String path, Map<String, Object> payload) {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(path)
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new AgentUnavailableException("AI 服务返回空响应");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new AgentUnavailableException(
                    "AI 服务调用失败: " + exception.getStatusCode().value(),
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw new AgentUnavailableException("无法连接 AI 服务", exception);
        }
    }

    private static String extractText(Map<?, ?> response) {
        Object text = response.get("text");
        if (text == null) {
            throw new AgentUnavailableException("AI 服务未返回文本");
        }
        return text.toString();
    }

    private static List<PublicAiCitation> extractCitations(Map<?, ?> response) {
        Object raw = response.get("citations");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<PublicAiCitation> citations = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object title = map.get("title");
            Object metadata = map.get("metadata");
            String url = "";
            if (metadata instanceof Map<?, ?> meta) {
                Object urlValue = meta.get("url");
                url = urlValue == null ? "" : urlValue.toString();
            }
            citations.add(new PublicAiCitation(title == null ? "" : title.toString(), url));
        }
        return List.copyOf(citations);
    }
}
