package com.linqibin.blog.post.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class SlugGenerator {

    private static final int MAX_SLUG_LENGTH = 120;
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");
    private final Supplier<String> fallbackIdSupplier;

    public SlugGenerator() {
        this(() -> java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    }

    public SlugGenerator(Supplier<String> fallbackIdSupplier) {
        this.fallbackIdSupplier = Objects.requireNonNull(fallbackIdSupplier);
    }

    public String generateFromTitle(String title) {
        String normalized = normalize(title);
        if (!normalized.isBlank()) {
            return normalized;
        }
        return trimToMaxLength("post-" + normalizeFallbackId());
    }

    public String normalizeRequestedSlug(String slug) {
        String normalized = normalize(slug);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("slug 不能为空");
        }
        return normalized;
    }

    public String ensureUnique(String baseSlug, Predicate<String> slugExists) {
        if (!slugExists.test(baseSlug)) {
            return baseSlug;
        }

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
        String lowerCase = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        String normalized = NON_ALPHANUMERIC.matcher(lowerCase).replaceAll("-");
        normalized = EDGE_HYPHENS.matcher(normalized).replaceAll("");
        return trimToMaxLength(normalized);
    }

    private String normalizeFallbackId() {
        return normalize(fallbackIdSupplier.get());
    }

    private String appendNumericSuffix(String slug, int suffix) {
        String suffixValue = "-" + suffix;
        int baseMaxLength = MAX_SLUG_LENGTH - suffixValue.length();
        String baseSlug = slug.length() > baseMaxLength ? slug.substring(0, baseMaxLength) : slug;
        baseSlug = EDGE_HYPHENS.matcher(baseSlug).replaceAll("");
        return baseSlug + suffixValue;
    }

    private String trimToMaxLength(String value) {
        return value.length() > MAX_SLUG_LENGTH ? value.substring(0, MAX_SLUG_LENGTH) : value;
    }
}
