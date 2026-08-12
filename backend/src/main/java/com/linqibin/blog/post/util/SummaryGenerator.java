package com.linqibin.blog.post.util;

// 摘要生成器：从 Markdown 原文中提取纯文本并截取指定长度的摘要。
public final class SummaryGenerator {

    private static final int DEFAULT_MAX_LENGTH = 160;

    private SummaryGenerator() {
    }

    // 生成默认长度的摘要。
    public static String generate(String markdown) {
        return generate(markdown, DEFAULT_MAX_LENGTH);
    }

    // 生成指定长度的摘要：先去除 Markdown 语法标记，再压成单段并截取前 N 个字符。
    public static String generate(String markdown, int maxLength) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String plainText = stripMarkdown(markdown).replaceAll("\\s+", " ").trim();
        if (plainText.isEmpty()) {
            return "";
        }
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        // 中文常无空格，优先在句读处截断；找不到再硬截。
        int cut = findCutIndex(plainText, maxLength);
        return plainText.substring(0, cut).trim() + "…";
    }

    // 去除 Markdown 语法标记，返回纯文本。同时用于摘要生成和阅读时长估算。
    public static String stripMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return markdown
                // (?m) 让 ^ 匹配每一行行首，否则只会清掉全文开头的标题。
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("!\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("_(.+?)_", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("(?m)^>\\s+", "")
                .replaceAll("(?m)^[\\-*+]\\s+", "")
                .replaceAll("(?m)^\\d+\\.\\s+", "")
                .replaceAll("(?m)^-{3,}\\s*$", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }

    private static int findCutIndex(String text, int maxLength) {
        String window = text.substring(0, Math.min(maxLength, text.length()));
        int[] preferred = {
                window.lastIndexOf('。'),
                window.lastIndexOf('！'),
                window.lastIndexOf('？'),
                window.lastIndexOf('；'),
                window.lastIndexOf('.'),
                window.lastIndexOf('!'),
                window.lastIndexOf('?'),
                window.lastIndexOf(' '),
                window.lastIndexOf('，'),
                window.lastIndexOf(','),
                window.lastIndexOf('、')
        };
        int best = -1;
        for (int index : preferred) {
            // 太靠前的截断没有意义，至少保留一半篇幅。
            if (index >= maxLength / 2 && index > best) {
                best = index;
            }
        }
        if (best > 0) {
            return best + 1;
        }
        return maxLength;
    }
}
