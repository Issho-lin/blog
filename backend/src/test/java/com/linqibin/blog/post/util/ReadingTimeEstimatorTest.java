package com.linqibin.blog.post.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadingTimeEstimatorTest {

    @Test
    void emptyTextReturnsMinimumOneMinute() {
        assertEquals(1, ReadingTimeEstimator.estimate(null));
        assertEquals(1, ReadingTimeEstimator.estimate(""));
        assertEquals(1, ReadingTimeEstimator.estimate("   "));
    }

    @Test
    void shortTextReturnsOneMinute() {
        assertEquals(1, ReadingTimeEstimator.estimate("Hello world"));
    }

    @Test
    void longEnglishTextEstimatesByWordCount() {
        // 200 个单词应该正好 1 分钟，400 个单词应该 2 分钟。
        String twoHundredWords = "word ".repeat(200).trim();
        assertEquals(1, ReadingTimeEstimator.estimate(twoHundredWords));

        String fourHundredWords = "word ".repeat(400).trim();
        assertEquals(2, ReadingTimeEstimator.estimate(fourHundredWords));
    }

    @Test
    void chineseTextCountsEachCharacterAsOneWord() {
        // 200 个中文字符应该 1 分钟。
        String twoHundredChars = "字".repeat(200);
        assertEquals(1, ReadingTimeEstimator.estimate(twoHundredChars));

        // 400 个中文字符应该 2 分钟。
        String fourHundredChars = "字".repeat(400);
        assertEquals(2, ReadingTimeEstimator.estimate(fourHundredChars));
    }

    @Test
    void mixedContentEstimatesCorrectly() {
        // 100 个中文字符 + 100 个英文单词 = 约 200 个有效词，应 1 分钟。
        String mixed = "字".repeat(100) + " " + "word ".repeat(100);
        int result = ReadingTimeEstimator.estimate(mixed);
        assertTrue(result >= 1);
    }
}
