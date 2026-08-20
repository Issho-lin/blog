package com.linqibin.blog.ai.application;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTaxonomyParserTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void parsePlainJson() {
        AiTaxonomyParser.Suggestion suggestion = AiTaxonomyParser.parse(
                "{\"category\":\"工程实践\",\"tags\":[\"Java\",\"Spring\"]}",
                jsonMapper
        );
        assertEquals("工程实践", suggestion.category());
        assertEquals(2, suggestion.tags().size());
        assertTrue(suggestion.tags().contains("Java"));
    }

    @Test
    void parseFencedJsonAndDedupesTags() {
        AiTaxonomyParser.Suggestion suggestion = AiTaxonomyParser.parse(
                """
                这是建议：
                ```json
                {"category":"后端","tags":["Redis","redis","缓存"]}
                ```
                """,
                jsonMapper
        );
        assertEquals("后端", suggestion.category());
        assertEquals(2, suggestion.tags().size());
        assertEquals("Redis", suggestion.tags().get(0));
        assertEquals("缓存", suggestion.tags().get(1));
    }
}
