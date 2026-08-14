package com.linqibin.blog.site.domain;

// 站点设置仓库：全站只有一份设置。
public interface SiteSettingsRepository {

    SiteSettings get();

    SiteSettings save(SiteSettings settings);
}
