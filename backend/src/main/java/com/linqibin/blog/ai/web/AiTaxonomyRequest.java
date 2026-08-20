package com.linqibin.blog.ai.web;

import jakarta.validation.constraints.Size;

public record AiTaxonomyRequest(
        @Size(max = 300, message = "标题过长")
        String title,
        @Size(max = 200_000, message = "正文过长")
        String markdown
) {
}
