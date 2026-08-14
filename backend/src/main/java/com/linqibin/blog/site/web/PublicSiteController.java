package com.linqibin.blog.site.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.markdown.MarkdownService;
import com.linqibin.blog.site.application.SiteSettingsService;

@RestController
@RequestMapping("/api/public/site")
public class PublicSiteController {

    private final SiteSettingsService siteSettingsService;
    private final MarkdownService markdownService;

    public PublicSiteController(SiteSettingsService siteSettingsService, MarkdownService markdownService) {
        this.siteSettingsService = siteSettingsService;
        this.markdownService = markdownService;
    }

    @GetMapping
    public SiteSettingsResponse get() {
        var settings = siteSettingsService.get();
        return SiteSettingsResponse.from(settings, markdownService.render(settings.aboutMarkdown()));
    }
}
