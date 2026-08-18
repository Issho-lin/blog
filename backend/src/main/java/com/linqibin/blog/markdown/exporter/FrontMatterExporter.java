package com.linqibin.blog.markdown.exporter;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

// Front Matter 导出器：从文章元数据生成带 YAML Front Matter 的 Markdown 文件内容。
// 导出格式示例：
// ---
// title: Hello World
// slug: hello-world
// excerpt: A short description
// cover: /images/cover.jpg
// category: Spring Boot
// tags:
//   - Java
//   - Spring
// published_at: 2026-08-11T10:00:00Z
// status: PUBLISHED
// updated_at: 2026-08-11T11:00:00Z
// ---
// # 正文内容
public class FrontMatterExporter {

    // 生成带 Front Matter 的 Markdown 文件内容。
    public String export(
            String title,
            String slug,
            String excerpt,
            String cover,
            String category,
            List<String> tags,
            String status,
            Instant publishedAt,
            Instant updatedAt,
            String markdownBody
    ) {
        return export(title, slug, excerpt, cover, category, tags, status, publishedAt, updatedAt,
                null, null, markdownBody);
    }

    public String export(
            String title,
            String slug,
            String excerpt,
            String cover,
            String category,
            List<String> tags,
            String status,
            Instant publishedAt,
            Instant updatedAt,
            String seoTitle,
            String seoDescription,
            String markdownBody
    ) {
        Objects.requireNonNull(title, "title 不能为空");
        Objects.requireNonNull(slug, "slug 不能为空");
        Objects.requireNonNull(markdownBody, "markdownBody 不能为空");

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        appendField(sb, "title", title);
        appendField(sb, "slug", slug);
        if (excerpt != null && !excerpt.isBlank()) {
            appendField(sb, "excerpt", excerpt);
        }
        if (cover != null && !cover.isBlank()) {
            appendField(sb, "cover", cover);
        }
        if (category != null && !category.isBlank()) {
            appendField(sb, "category", category);
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append("tags:\n");
            for (String tag : tags) {
                sb.append("  - ").append(escapeYamlValue(tag)).append("\n");
            }
        }
        if (publishedAt != null) {
            appendField(sb, "published_at", publishedAt.toString());
        }
        appendField(sb, "status", status);
        if (seoTitle != null && !seoTitle.isBlank()) {
            appendField(sb, "seo_title", seoTitle);
        }
        if (seoDescription != null && !seoDescription.isBlank()) {
            appendField(sb, "seo_description", seoDescription);
        }
        if (updatedAt != null) {
            appendField(sb, "updated_at", updatedAt.toString());
        }
        sb.append("---\n\n");
        sb.append(markdownBody);
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value) {
        sb.append(key).append(": ").append(escapeYamlValue(value)).append("\n");
    }

    // 简单的 YAML 值转义：处理含特殊字符的字符串。
    private String escapeYamlValue(String value) {
        if (value == null) {
            return "\"\"";
        }
        // 如果值包含冒号后跟空格、以特殊字符开头或包含引号，用双引号包裹。
        if (value.contains(": ") || value.startsWith("#")
                || value.startsWith("-") || value.startsWith("[")
                || value.startsWith("{") || value.startsWith("!")
                || value.startsWith("&") || value.startsWith("*")
                || value.startsWith("?") || value.startsWith("|")
                || value.startsWith(">") || value.startsWith("'\"")
                || value.startsWith("@") || value.startsWith("`")
                || value.startsWith(" ") || value.endsWith(" ")) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}
