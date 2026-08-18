package com.linqibin.blog.media.application;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linqibin.blog.post.domain.Post;

// 从正文和封面中提取本站 /uploads/{uuid}.{ext} 文件名，供删除时判断引用。
public final class LocalMediaReferences {

    private static final Pattern LOCAL_FILE = Pattern.compile(
            "/uploads/([0-9a-fA-F]{32}\\.(?:jpe?g|png|gif|webp))",
            Pattern.CASE_INSENSITIVE
    );

    private LocalMediaReferences() {
    }

    public static Set<String> filenamesIn(Post post) {
        if (post == null) {
            return Set.of();
        }
        return filenamesIn(post.markdownContent(), post.coverUrl());
    }

    public static Set<String> filenamesIn(String... texts) {
        Set<String> names = new LinkedHashSet<>();
        if (texts == null) {
            return names;
        }
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = LOCAL_FILE.matcher(text);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }
}
