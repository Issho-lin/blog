package com.linqibin.blog.post.util;

// 摘要生成器：从 Markdown 原文中提取纯文本并截取指定长度的摘要。
public final class SummaryGenerator {

    private static final int DEFAULT_MAX_LENGTH = 200;

    private SummaryGenerator() {
    }

    // 生成默认长度的摘要（200 字符）。
    public static String generate(String markdown) {
        return generate(markdown, DEFAULT_MAX_LENGTH);
    }

    // 生成指定长度的摘要：先去除 Markdown 语法标记，再截取前 N 个字符。
    public static String generate(String markdown, int maxLength) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String plainText = stripMarkdown(markdown);
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        // 在 maxLength 范围内尽量在空格处截断，避免截断半个词。
        int cut = plainText.lastIndexOf(' ', maxLength);
        if (cut <= 0) {
            cut = maxLength;
        }
        return plainText.substring(0, cut) + "…";
    }

    // 去除 Markdown 语法标记，返回纯文本。同时用于摘要生成和阅读时长估算。
    public static String stripMarkdown(String markdown) {
        return markdown
                // 去除标题标记。
                .replaceAll("^#{1,6}\\s+", "")
                // 去除图片和链接，保留文本。
                .replaceAll("!\\[([^]]*)]\\([^)]*\\)", "$1")
                .replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1")
                // 去除加粗、斜体和删除线标记。
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("_(.+?)_", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                // 去除行内代码和代码块标记。
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`([^`]+)`", "$1")
                // 去除引用标记。
                .replaceAll("^>\\s+", "")
                // 去除列表标记。
                .replaceAll("^[\\-*+]\\s+", "")
                .replaceAll("^\\d+\\.\\s+", "")
                // 去除水平分隔线。
                .replaceAll("^---+$", "")
                // 去除 HTML 标签。
                .replaceAll("<[^>]+>", "")
                // 压缩多余空白。
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }
}
