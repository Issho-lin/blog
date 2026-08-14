package com.linqibin.blog.site.web;

import java.time.Instant;

import com.linqibin.blog.site.domain.SiteSettings;

public record SiteSettingsResponse(
        String siteName,
        String siteSubtitle,
        String siteDescription,
        String authorName,
        String authorAvatarUrl,
        String aboutMarkdown,
        String aboutHtml,
        int postsPerPage,
        String timezone,
        String defaultLanguage,
        String faviconUrl,
        String defaultShareImageUrl,
        Instant updatedAt
) {

    public static SiteSettingsResponse from(SiteSettings settings, String aboutHtml) {
        return new SiteSettingsResponse(
                settings.siteName(),
                settings.siteSubtitle(),
                settings.siteDescription(),
                settings.authorName(),
                settings.authorAvatarUrl(),
                settings.aboutMarkdown(),
                aboutHtml,
                settings.postsPerPage(),
                settings.timezone(),
                settings.defaultLanguage(),
                settings.faviconUrl(),
                settings.defaultShareImageUrl(),
                settings.updatedAt()
        );
    }
}
