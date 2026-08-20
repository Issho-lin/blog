package com.linqibin.blog.ai.application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.linqibin.blog.ai.exception.AgentUnavailableException;
import com.linqibin.blog.ai.infrastructure.AgentClient;
import com.linqibin.blog.ai.web.AiTaxonomyResponse;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.Tag;

@Service
public class AiTaxonomyService {

    private static final int MAX_MARKDOWN = 8000;

    private final AgentClient agentClient;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final JsonMapper jsonMapper;

    public AiTaxonomyService(
            AgentClient agentClient,
            CategoryService categoryService,
            TagService tagService,
            JsonMapper jsonMapper
    ) {
        this.agentClient = agentClient;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.jsonMapper = jsonMapper;
    }

    public AiTaxonomyResponse suggest(String title, String markdown) {
        String body = markdown == null ? "" : markdown;
        if (body.length() > MAX_MARKDOWN) {
            body = body.substring(0, MAX_MARKDOWN);
        }
        String catalog = buildCatalog();
        String raw = agentClient.complete("taxonomy", body, catalog, null, title == null ? "" : title);
        return apply(AiTaxonomyParser.parse(raw, jsonMapper));
    }

    AiTaxonomyResponse apply(AiTaxonomyParser.Suggestion suggestion) {
        if (suggestion.category().isBlank() && suggestion.tags().isEmpty()) {
            throw new AgentUnavailableException("AI 未能给出分类或标签");
        }
        AssignedCategory assignedCategory = suggestion.category().isBlank()
                ? AssignedCategory.empty()
                : resolveCategory(suggestion.category());
        List<AiTaxonomyResponse.AssignedTag> tags = new ArrayList<>();
        for (String name : suggestion.tags()) {
            Tag existing = tagService.findByName(name).orElse(null);
            if (existing != null) {
                tags.add(new AiTaxonomyResponse.AssignedTag(existing.id(), existing.name(), false));
                continue;
            }
            Tag created = tagService.create(name, null);
            tags.add(new AiTaxonomyResponse.AssignedTag(created.id(), created.name(), true));
        }
        return new AiTaxonomyResponse(
                assignedCategory.id(),
                assignedCategory.name(),
                assignedCategory.created(),
                List.copyOf(tags)
        );
    }

    private AssignedCategory resolveCategory(String name) {
        return categoryService.findByName(name)
                .map(category -> new AssignedCategory(category.id(), category.name(), false))
                .orElseGet(() -> {
                    Category created = categoryService.create(name, null, "");
                    return new AssignedCategory(created.id(), created.name(), true);
                });
    }

    private String buildCatalog() {
        String categories = categoryService.findAll().stream()
                .map(Category::name)
                .collect(Collectors.joining("、"));
        String tags = tagService.findAll().stream()
                .map(Tag::name)
                .collect(Collectors.joining("、"));
        return "已有分类：" + (categories.isBlank() ? "（无）" : categories)
                + "\n已有标签：" + (tags.isBlank() ? "（无）" : tags);
    }

    private record AssignedCategory(java.util.UUID id, String name, boolean created) {
        static AssignedCategory empty() {
            return new AssignedCategory(null, null, false);
        }
    }
}
