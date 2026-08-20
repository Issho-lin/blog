package com.linqibin.blog.ai.application;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.linqibin.blog.ai.exception.AgentUnavailableException;

final class AiTaxonomyParser {

    private AiTaxonomyParser() {
    }

    static Suggestion parse(String raw, JsonMapper jsonMapper) {
        String json = extractJson(raw);
        JsonNode root;
        try {
            root = jsonMapper.readTree(json);
        } catch (RuntimeException exception) {
            throw new AgentUnavailableException("AI 未能给出有效分类", exception);
        }
        if (root == null || !root.isObject()) {
            throw new AgentUnavailableException("AI 未能给出有效分类");
        }
        String category = text(root.get("category"));
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = root.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode item : tagsNode) {
                String name = text(item);
                if (!name.isBlank() && tags.stream().noneMatch(existing -> existing.equalsIgnoreCase(name))) {
                    tags.add(name);
                }
                if (tags.size() >= 5) {
                    break;
                }
            }
        }
        return new Suggestion(category, List.copyOf(tags));
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int fence = trimmed.indexOf("```");
        if (fence >= 0) {
            int start = trimmed.indexOf('\n', fence);
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) {
                trimmed = trimmed.substring(start + 1, end).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || !node.isValueNode()) {
            return "";
        }
        return node.asString().trim();
    }

    record Suggestion(String category, List<String> tags) {
    }
}
