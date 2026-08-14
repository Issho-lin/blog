package com.linqibin.blog.site.infrastructure.persistence;

import com.linqibin.blog.site.domain.SiteSettings;

public class SiteSettingsEntityMapper {

    public SiteSettingsEntity toEntity(SiteSettings settings) {
        return new SiteSettingsEntity(
                (short) SiteSettings.SINGLETON_ID,
                settings.siteName(),
                settings.siteSubtitle(),
                settings.siteDescription(),
                settings.authorName(),
                settings.authorAvatarUrl(),
                settings.aboutMarkdown(),
                settings.postsPerPage(),
                settings.timezone(),
                settings.defaultLanguage(),
                settings.faviconUrl(),
                settings.defaultShareImageUrl(),
                settings.updatedAt()
        );
    }

    public SiteSettings toDomain(SiteSettingsEntity entity) {
        return new SiteSettings(
                entity.getSiteName(),
                entity.getSiteSubtitle(),
                entity.getSiteDescription(),
                entity.getAuthorName(),
                entity.getAuthorAvatarUrl(),
                entity.getAboutMarkdown(),
                entity.getPostsPerPage(),
                entity.getTimezone(),
                entity.getDefaultLanguage(),
                entity.getFaviconUrl(),
                entity.getDefaultShareImageUrl(),
                entity.getUpdatedAt()
        );
    }
}
