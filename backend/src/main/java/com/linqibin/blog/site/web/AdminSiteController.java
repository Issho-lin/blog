package com.linqibin.blog.site.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.markdown.MarkdownService;
import com.linqibin.blog.site.application.SiteSettingsService;
import com.linqibin.blog.site.domain.SiteSettings;

@RestController
@RequestMapping("/api/admin/site")
public class AdminSiteController {

    private final SiteSettingsService siteSettingsService;
    private final MarkdownService markdownService;

    public AdminSiteController(SiteSettingsService siteSettingsService, MarkdownService markdownService) {
        this.siteSettingsService = siteSettingsService;
        this.markdownService = markdownService;
    }

    @GetMapping
    public SiteSettingsResponse get() {
        SiteSettings settings = siteSettingsService.get();
        return SiteSettingsResponse.from(settings, markdownService.render(settings.aboutMarkdown()));
    }

    @PutMapping
    public SiteSettingsResponse update(@Valid @RequestBody UpdateSiteSettingsRequest request) {
        SiteSettings settings = siteSettingsService.update(
                request.siteName(),
                request.siteSubtitle(),
                request.siteDescription(),
                request.authorName(),
                request.authorAvatarUrl(),
                request.aboutMarkdown(),
                request.postsPerPage(),
                request.timezone(),
                request.defaultLanguage(),
                request.faviconUrl(),
                request.defaultShareImageUrl()
        );
        return SiteSettingsResponse.from(settings, markdownService.render(settings.aboutMarkdown()));
    }
}
