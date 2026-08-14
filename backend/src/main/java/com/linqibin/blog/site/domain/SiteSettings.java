package com.linqibin.blog.site.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

// 站点设置：全站只有一份，公开页和管理端共用同一份数据。
public record SiteSettings(
        String siteName,
        String siteSubtitle,
        String siteDescription,
        String authorName,
        String authorAvatarUrl,
        String aboutMarkdown,
        int postsPerPage,
        String timezone,
        String defaultLanguage,
        String faviconUrl,
        String defaultShareImageUrl,
        Instant updatedAt
) {

    public static final int SINGLETON_ID = 1;

    public SiteSettings {
        Objects.requireNonNull(siteName, "站点名称不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        siteName = siteName.trim();
        siteSubtitle = blankToEmpty(siteSubtitle);
        siteDescription = blankToEmpty(siteDescription);
        authorName = blankToEmpty(authorName);
        authorAvatarUrl = blankToEmpty(authorAvatarUrl);
        aboutMarkdown = aboutMarkdown == null ? "" : aboutMarkdown;
        timezone = blankToEmpty(timezone);
        defaultLanguage = blankToEmpty(defaultLanguage);
        faviconUrl = blankToEmpty(faviconUrl);
        defaultShareImageUrl = blankToEmpty(defaultShareImageUrl);

        if (siteName.isBlank()) {
            throw new IllegalArgumentException("站点名称不能为空");
        }
        if (siteName.length() > 100) {
            throw new IllegalArgumentException("站点名称不能超过 100 个字符");
        }
        if (postsPerPage < 1 || postsPerPage > 100) {
            throw new IllegalArgumentException("首页每页文章数需在 1 到 100 之间");
        }
        if (timezone.isBlank()) {
            timezone = "Asia/Shanghai";
        }
        try {
            ZoneId.of(timezone);
        } catch (Exception exception) {
            throw new IllegalArgumentException("时区无效");
        }
        if (defaultLanguage.isBlank()) {
            defaultLanguage = "zh-CN";
        }
    }

    public static SiteSettings defaults(Instant now) {
        return new SiteSettings(
                "Linqibin Blog",
                "書齋 · 技術手稿",
                "记录技术学习与工程实践。写给自己，也留给路过的人。",
                "",
                "",
                "这里是个人技术博客。\n\n记录学习、工程实践与写作。",
                20,
                "Asia/Shanghai",
                "zh-CN",
                "",
                "",
                now
        );
    }

    public SiteSettings update(
            String siteName,
            String siteSubtitle,
            String siteDescription,
            String authorName,
            String authorAvatarUrl,
            String aboutMarkdown,
            int postsPerPage,
            String timezone,
            String defaultLanguage,
            String faviconUrl,
            String defaultShareImageUrl,
            Instant now
    ) {
        return new SiteSettings(
                siteName,
                siteSubtitle,
                siteDescription,
                authorName,
                authorAvatarUrl,
                aboutMarkdown,
                postsPerPage,
                timezone,
                defaultLanguage,
                faviconUrl,
                defaultShareImageUrl,
                now
        );
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
