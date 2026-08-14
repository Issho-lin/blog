package com.linqibin.blog.site.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "site_settings")
public class SiteSettingsEntity {

    @Id
    @Column(columnDefinition = "smallint")
    private Short id;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;

    @Column(name = "site_subtitle", length = 200)
    private String siteSubtitle;

    @Column(name = "site_description", length = 500)
    private String siteDescription;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "author_avatar_url", length = 500)
    private String authorAvatarUrl;

    @Column(name = "about_markdown", columnDefinition = "text")
    private String aboutMarkdown;

    @Column(name = "posts_per_page", nullable = false)
    private int postsPerPage;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "default_language", nullable = false, length = 16)
    private String defaultLanguage;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "default_share_image_url", length = 500)
    private String defaultShareImageUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SiteSettingsEntity() {
    }

    public SiteSettingsEntity(
            Short id,
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
            String defaultShareImageUrl,
            Instant updatedAt
    ) {
        this.id = id;
        this.siteName = siteName;
        this.siteSubtitle = siteSubtitle;
        this.siteDescription = siteDescription;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.aboutMarkdown = aboutMarkdown;
        this.postsPerPage = postsPerPage;
        this.timezone = timezone;
        this.defaultLanguage = defaultLanguage;
        this.faviconUrl = faviconUrl;
        this.defaultShareImageUrl = defaultShareImageUrl;
        this.updatedAt = updatedAt;
    }

    public Short getId() {
        return id;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getSiteSubtitle() {
        return siteSubtitle;
    }

    public String getSiteDescription() {
        return siteDescription;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getAboutMarkdown() {
        return aboutMarkdown;
    }

    public int getPostsPerPage() {
        return postsPerPage;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public String getDefaultShareImageUrl() {
        return defaultShareImageUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
