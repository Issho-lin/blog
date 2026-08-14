package com.linqibin.blog.site.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSiteSettingsRepository extends JpaRepository<SiteSettingsEntity, Short> {
}
