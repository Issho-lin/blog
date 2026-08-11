package com.linqibin.blog.post.util;

// 阅读时长估算：按平均阅读速度 200 词/分钟计算，最少 1 分钟。
public final class ReadingTimeEstimator {

    private static final int WORDS_PER_MINUTE = 200;
    private static final int MIN_MINUTES = 1;

    private ReadingTimeEstimator() {
    }

    // 根据纯文本字数估算阅读时长（分钟）。
    public static int estimate(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return MIN_MINUTES;
        }
        String trimmed = plainText.trim();
        // CJK 字符每个算一个词，单独统计。
        long cjkCount = trimmed.chars()
                .filter(c -> isCjkCharacter((char) c))
                .count();
        // 去掉 CJK 字符后按空格分割统计非 CJK 词数，避免双重计算。
        String nonCjkText = trimmed.replaceAll("[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF]", " ");
        int nonCjkWordCount = 0;
        for (String word : nonCjkText.split("\\s+")) {
            if (!word.isBlank()) {
                nonCjkWordCount++;
            }
        }
        int effectiveWords = (int) (cjkCount + nonCjkWordCount);
        if (effectiveWords <= 0) {
            return MIN_MINUTES;
        }
        return Math.max(MIN_MINUTES, (int) Math.ceil((double) effectiveWords / WORDS_PER_MINUTE));
    }

    private static boolean isCjkCharacter(char c) {
        // CJK 统一表意文字范围。
        return (c >= '\u4E00' && c <= '\u9FFF')
                || (c >= '\u3400' && c <= '\u4DBF')
                || (c >= '\uF900' && c <= '\uFAFF');
    }
}
