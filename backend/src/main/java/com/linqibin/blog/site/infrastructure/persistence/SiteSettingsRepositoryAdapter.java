package com.linqibin.blog.site.infrastructure.persistence;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import com.linqibin.blog.site.domain.SiteSettings;
import com.linqibin.blog.site.domain.SiteSettingsRepository;

@Transactional
public class SiteSettingsRepositoryAdapter implements SiteSettingsRepository {

    private final SpringDataSiteSettingsRepository springDataSiteSettingsRepository;
    private final SiteSettingsEntityMapper siteSettingsEntityMapper;

    public SiteSettingsRepositoryAdapter(
            SpringDataSiteSettingsRepository springDataSiteSettingsRepository,
            SiteSettingsEntityMapper siteSettingsEntityMapper
    ) {
        this.springDataSiteSettingsRepository = springDataSiteSettingsRepository;
        this.siteSettingsEntityMapper = siteSettingsEntityMapper;
    }

    @Override
    public SiteSettings get() {
        return springDataSiteSettingsRepository.findById((short) SiteSettings.SINGLETON_ID)
                .map(siteSettingsEntityMapper::toDomain)
                .orElseGet(() -> {
                    SiteSettings defaults = SiteSettings.defaults(Instant.now());
                    return save(defaults);
                });
    }

    @Override
    public SiteSettings save(SiteSettings settings) {
        SiteSettingsEntity saved = springDataSiteSettingsRepository.save(
                siteSettingsEntityMapper.toEntity(settings)
        );
        return siteSettingsEntityMapper.toDomain(saved);
    }
}
