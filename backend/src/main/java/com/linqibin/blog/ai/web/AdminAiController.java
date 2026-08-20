package com.linqibin.blog.ai.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.ai.application.AiCorpusSync;
import com.linqibin.blog.ai.application.AiSettingsService;
import com.linqibin.blog.ai.application.AiTaxonomyService;
import com.linqibin.blog.ai.infrastructure.AgentClient;
import com.linqibin.blog.post.application.PostService;
import com.linqibin.blog.post.domain.Post;

@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {

    private final AgentClient agentClient;
    private final AiCorpusSync aiCorpusSync;
    private final PostService postService;
    private final AiSettingsService aiSettingsService;
    private final AiTaxonomyService aiTaxonomyService;

    public AdminAiController(
            AgentClient agentClient,
            AiCorpusSync aiCorpusSync,
            PostService postService,
            AiSettingsService aiSettingsService,
            AiTaxonomyService aiTaxonomyService
    ) {
        this.agentClient = agentClient;
        this.aiCorpusSync = aiCorpusSync;
        this.postService = postService;
        this.aiSettingsService = aiSettingsService;
        this.aiTaxonomyService = aiTaxonomyService;
    }

    @GetMapping("/settings")
    public AiSettingsResponse getSettings() {
        return AiSettingsResponse.from(aiSettingsService.get());
    }

    @PutMapping("/settings")
    public AiSettingsResponse updateSettings(@Valid @RequestBody UpdateAiSettingsRequest request) {
        return AiSettingsResponse.from(aiSettingsService.update(
                request.enabled(),
                request.assistantEnabled(),
                request.chatBaseUrl(),
                request.chatApiKey(),
                request.chatModel(),
                request.embedBaseUrl(),
                request.embedApiKey(),
                request.embedModel(),
                request.embedDimensions(),
                request.assistantPersona(),
                request.ratePerMinute(),
                request.ratePerDay()
        ));
    }

    @PostMapping("/summarize")
    public AiTextResponse summarize(@Valid @RequestBody AiSummarizeRequest request) {
        String text = agentClient.complete("summarize", request.markdown(), "", null, "");
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        return new AiTextResponse(text);
    }

    @PostMapping("/write")
    public AiTextResponse write(@Valid @RequestBody AiWriteRequest request) {
        String markdown = request.markdown() == null ? "" : request.markdown();
        String instruction = request.instruction() == null ? "" : request.instruction();
        String mode = request.mode() == null || request.mode().isBlank() ? "continue" : request.mode();
        String text = agentClient.complete("write", markdown, instruction, mode, "");
        return new AiTextResponse(text);
    }

    @PostMapping("/taxonomy")
    public AiTaxonomyResponse taxonomy(@Valid @RequestBody AiTaxonomyRequest request) {
        return aiTaxonomyService.suggest(
                request.title() == null ? "" : request.title(),
                request.markdown() == null ? "" : request.markdown()
        );
    }

    @PostMapping("/index/rebuild")
    public AiRebuildResponse rebuildIndex() {
        int indexed = 0;
        for (Post post : postService.findAllPublishedPosts()) {
            aiCorpusSync.upsert(post);
            indexed++;
        }
        return new AiRebuildResponse(indexed);
    }
}
