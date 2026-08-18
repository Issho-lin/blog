package com.linqibin.blog.markdown.parser;

import java.util.List;
import java.util.Map;

// Markdown Front Matter 解析结果：包含解析出的元数据和剩余正文。
// 只保留首期需要识别的字段，未知字段被忽略而非报错。
public record FrontMatter(
        String title,
        String slug,
        String description,
        String cover,
        String date,
        String category,
        List<String> tags,
        String status,
        String seoTitle,
        String seoDescription
) {

    // 从 SnakeYAML 解析出的原始 Map 中提取已知字段。
    // 支持 description/excerpt 和 date/published_at 等别名。
    @SuppressWarnings("unchecked")
    public static FrontMatter fromMap(Map<String, Object> map) {
        if (map == null) {
            return new FrontMatter(null, null, null, null, null, null, List.of(), null, null, null);
        }

        String title = getString(map, "title");
        String slug = getString(map, "slug");
        // description 优先，excerpt 作为别名。
        String description = getString(map, "description");
        if (description == null) {
            description = getString(map, "excerpt");
        }
        String cover = getString(map, "cover");
        // date 优先，published_at 作为别名。
        String date = getString(map, "date");
        if (date == null) {
            date = getString(map, "published_at");
        }
        String category = getString(map, "category");
        String status = getString(map, "status");
        String seoTitle = getString(map, "seo_title");
        if (seoTitle == null) {
            seoTitle = getString(map, "seoTitle");
        }
        String seoDescription = getString(map, "seo_description");
        if (seoDescription == null) {
            seoDescription = getString(map, "seoDescription");
        }

        // tags 可以是 YAML 列表或逗号分隔的字符串。
        List<String> tags = List.of();
        Object rawTags = map.get("tags");
        if (rawTags instanceof List<?> list) {
            tags = list.stream()
                    .filter(obj -> obj != null)
                    .map(String::valueOf)
                    .toList();
        } else if (rawTags instanceof String s && !s.isBlank()) {
            tags = List.of(s.split(",")).stream()
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .toList();
        }

        return new FrontMatter(title, slug, description, cover, date, category, tags, status,
                seoTitle, seoDescription);
    }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        // SnakeYAML 会把 YAML 日期/时间戳解析成 java.util.Date，需要还原成字符串。
        if (value instanceof java.util.Date date) {
            return formatDate(date);
        }
        return String.valueOf(value);
    }

    // 把 SnakeYAML 解析出的 java.util.Date 还原成原始字符串格式。
    // 纯日期（如 2026-01-01）被解析为 UTC 午夜，还原为 yyyy-MM-dd；
    // 时间戳（如 2026-08-11T10:00:00Z）还原为 ISO-8601 格式。
    private static String formatDate(java.util.Date date) {
        java.time.Instant instant = date.toInstant();
        var zdt = instant.atZone(java.time.ZoneOffset.UTC);
        if (zdt.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return zdt.toLocalDate().toString();
        }
        return instant.toString();
    }
}
