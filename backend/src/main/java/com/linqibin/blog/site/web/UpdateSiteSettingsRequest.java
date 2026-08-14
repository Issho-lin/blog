package com.linqibin.blog.site.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSiteSettingsRequest(
        @NotBlank(message = "站点名称不能为空")
        @Size(max = 100, message = "站点名称不能超过 100 个字符")
        String siteName,
        @Size(max = 200, message = "站点副标题不能超过 200 个字符")
        String siteSubtitle,
        @Size(max = 500, message = "站点简介不能超过 500 个字符")
        String siteDescription,
        @Size(max = 100, message = "作者名称不能超过 100 个字符")
        String authorName,
        @Size(max = 500, message = "头像地址过长")
        String authorAvatarUrl,
        String aboutMarkdown,
        @Min(value = 1, message = "首页每页文章数需在 1 到 100 之间")
        @Max(value = 100, message = "首页每页文章数需在 1 到 100 之间")
        int postsPerPage,
        @NotBlank(message = "时区不能为空")
        String timezone,
        @NotBlank(message = "默认语言不能为空")
        String defaultLanguage,
        @Size(max = 500, message = "favicon 地址过长")
        String faviconUrl,
        @Size(max = 500, message = "默认分享图地址过长")
        String defaultShareImageUrl
) {
}
