package com.linqibin.blog.site.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.linqibin.blog.site.domain.SiteSettings;
import com.linqibin.blog.site.domain.SiteSettingsRepository;

public class SiteSettingsService {

    private final SiteSettingsRepository siteSettingsRepository;
    private final Clock clock;

    public SiteSettingsService(SiteSettingsRepository siteSettingsRepository, Clock clock) {
        this.siteSettingsRepository = Objects.requireNonNull(siteSettingsRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public SiteSettings get() {
        return siteSettingsRepository.get();
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
            String defaultShareImageUrl
    ) {
        Instant now = Instant.now(clock);
        SiteSettings updated = siteSettingsRepository.get().update(
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
        return siteSettingsRepository.save(updated);
    }
}
