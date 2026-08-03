package com.linqibin.blog.post.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// slug 生成器：负责把标题或用户输入转换成可放进 URL 的稳定别名。
public class SlugGenerator {

    // slug 最长 120 个字符，避免 URL 过长。
    private static final int MAX_SLUG_LENGTH = 120;
    // 把连续的非字母数字字符压成一个短横线。
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    // 去掉开头和结尾多余的短横线。
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");
    private final Supplier<String> fallbackIdSupplier;

    public SlugGenerator() {
        // 默认兜底值使用短 UUID，保证纯中文标题这类场景也能生成 slug。
        this(() -> java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    }

    public SlugGenerator(Supplier<String> fallbackIdSupplier) {
        this.fallbackIdSupplier = Objects.requireNonNull(fallbackIdSupplier);
    }

    public String generateFromTitle(String title) {
        // 能从标题直接得到 slug 时优先使用标题语义，便于阅读和 SEO。
        String normalized = normalize(title);
        if (!normalized.isBlank()) {
            return normalized;
        }
        // 标题转完为空时，退化成 post-随机串，保证总能生成可用 slug。
        return trimToMaxLength("post-" + normalizeFallbackId());
    }

    public String normalizeRequestedSlug(String slug) {
        // 用户手动输入的 slug 也要走统一清洗规则，避免格式五花八门。
        String normalized = normalize(slug);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("slug 不能为空");
        }
        return normalized;
    }

    public String ensureUnique(String baseSlug, Predicate<String> slugExists) {
        // 如果当前 slug 没被占用，直接返回，避免多余改写。
        if (!slugExists.test(baseSlug)) {
            return baseSlug;
        }

        // 已存在时不断补数字后缀，直到找到一个未占用的 slug。
        int suffix = 2;
        while (true) {
            String candidate = appendNumericSuffix(baseSlug, suffix);
            if (!slugExists.test(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    private String normalize(String rawValue) {
        // 规范化流程：去空格 -> 转小写 -> 非字母数字改成短横线 -> 去掉两端短横线。
        String lowerCase = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        String normalized = NON_ALPHANUMERIC.matcher(lowerCase).replaceAll("-");
        normalized = EDGE_HYPHENS.matcher(normalized).replaceAll("");
        return trimToMaxLength(normalized);
    }

    private String normalizeFallbackId() {
        // 兜底随机串也走同一套规范，保证生成结果格式统一。
        return normalize(fallbackIdSupplier.get());
    }

    private String appendNumericSuffix(String slug, int suffix) {
        // 追加数字后缀前先给基础 slug 预留长度，避免超出最大长度限制。
        String suffixValue = "-" + suffix;
        int baseMaxLength = MAX_SLUG_LENGTH - suffixValue.length();
        String baseSlug = slug.length() > baseMaxLength ? slug.substring(0, baseMaxLength) : slug;
        baseSlug = EDGE_HYPHENS.matcher(baseSlug).replaceAll("");
        return baseSlug + suffixValue;
    }

    private String trimToMaxLength(String value) {
        // 截断逻辑统一放在这里，避免各处重复写 substring。
        return value.length() > MAX_SLUG_LENGTH ? value.substring(0, MAX_SLUG_LENGTH) : value;
    }
}
