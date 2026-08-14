package com.linqibin.blog.site.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.site.application.SiteSettingsService;
import com.linqibin.blog.site.domain.SiteSettingsRepository;
import com.linqibin.blog.site.infrastructure.persistence.SiteSettingsEntityMapper;
import com.linqibin.blog.site.infrastructure.persistence.SiteSettingsRepositoryAdapter;
import com.linqibin.blog.site.infrastructure.persistence.SpringDataSiteSettingsRepository;

@Configuration
public class SiteModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.site",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemorySiteSettingsRepository inMemorySiteSettingsRepository() {
        return new InMemorySiteSettingsRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.site", name = "repository-type", havingValue = "jpa")
    public SiteSettingsEntityMapper siteSettingsEntityMapper() {
        return new SiteSettingsEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.site", name = "repository-type", havingValue = "jpa")
    public SiteSettingsRepository jpaSiteSettingsRepositoryAdapter(
            SpringDataSiteSettingsRepository springDataSiteSettingsRepository,
            SiteSettingsEntityMapper siteSettingsEntityMapper
    ) {
        return new SiteSettingsRepositoryAdapter(springDataSiteSettingsRepository, siteSettingsEntityMapper);
    }

    @Bean
    public SiteSettingsService siteSettingsService(
            SiteSettingsRepository siteSettingsRepository,
            Clock clock
    ) {
        return new SiteSettingsService(siteSettingsRepository, clock);
    }
}
