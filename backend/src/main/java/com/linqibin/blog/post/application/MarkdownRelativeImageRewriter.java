package com.linqibin.blog.post.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 找出 Markdown 中的相对路径图片引用，并按文件名映射替换为已上传 URL。
final class MarkdownRelativeImageRewriter {

    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");

    private MarkdownRelativeImageRewriter() {
    }

    static boolean isRemoteOrAbsolute(String url) {
        String value = url.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("data:")
                || lower.startsWith("mailto:")
                || value.startsWith("/")
                || value.startsWith("#");
    }

    static String normalizePath(String url) {
        String value = url.trim().replace('\\', '/');
        while (value.startsWith("./")) {
            value = value.substring(2);
        }
        return value;
    }

    static String basename(String path) {
        String normalized = normalizePath(path);
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    static Set<String> findRelativeRefs(String markdown) {
        Set<String> refs = new LinkedHashSet<>();
        if (markdown == null || markdown.isBlank()) {
            return refs;
        }
        Matcher matcher = IMAGE.matcher(markdown);
        while (matcher.find()) {
            String url = matcher.group(2);
            if (!isRemoteOrAbsolute(url)) {
                refs.add(url);
            }
        }
        return refs;
    }

    static String rewrite(String markdown, Map<String, String> uploadedByKey, List<String> missing) {
        if (markdown == null || markdown.isBlank()) {
            return markdown == null ? "" : markdown;
        }
        Matcher matcher = IMAGE.matcher(markdown);
        StringBuilder rewritten = new StringBuilder();
        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);
            String replacement = "![" + alt + "](" + url + ")";
            if (!isRemoteOrAbsolute(url)) {
                String mapped = resolveUploaded(url, uploadedByKey);
                if (mapped != null) {
                    replacement = "![" + alt + "](" + mapped + ")";
                } else {
                    missing.add(url);
                }
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    static String resolveUploaded(String ref, Map<String, String> uploadedByKey) {
        String normalized = normalizePath(ref);
        if (uploadedByKey.containsKey(normalized)) {
            return uploadedByKey.get(normalized);
        }
        String base = basename(ref);
        return uploadedByKey.get(base);
    }
}
