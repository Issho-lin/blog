package com.linqibin.blog.ai.web;

import java.util.List;
import java.util.UUID;

public record AiTaxonomyResponse(
        UUID categoryId,
        String categoryName,
        boolean categoryCreated,
        List<AssignedTag> tags
) {
    public record AssignedTag(UUID id, String name, boolean created) {
    }
}
