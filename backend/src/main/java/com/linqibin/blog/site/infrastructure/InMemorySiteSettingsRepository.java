package com.linqibin.blog.site.infrastructure;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.linqibin.blog.site.domain.SiteSettings;
import com.linqibin.blog.site.domain.SiteSettingsRepository;

public class InMemorySiteSettingsRepository implements SiteSettingsRepository {

    private final AtomicReference<SiteSettings> settings =
            new AtomicReference<>(SiteSettings.defaults(Instant.parse("2026-01-01T00:00:00Z")));

    public void reset() {
        settings.set(SiteSettings.defaults(Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Override
    public SiteSettings get() {
        return settings.get();
    }

    @Override
    public SiteSettings save(SiteSettings next) {
        settings.set(next);
        return next;
    }
}
