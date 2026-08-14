import type { SiteSettings } from "@/lib/api/types";
import { getPublicSiteSettings } from "@/lib/api/posts";

export const fallbackSiteSettings: SiteSettings = {
  siteName: process.env.NEXT_PUBLIC_SITE_NAME ?? "Linqibin Blog",
  siteSubtitle: "書齋 · 技術手稿",
  siteDescription: "记录技术学习与工程实践。写给自己，也留给路过的人。",
  authorName: "",
  authorAvatarUrl: "",
  aboutMarkdown: "",
  aboutHtml: "",
  postsPerPage: 20,
  timezone: "Asia/Shanghai",
  defaultLanguage: "zh-CN",
  faviconUrl: "",
  defaultShareImageUrl: "",
  updatedAt: null,
};

export async function loadPublicSiteSettings(): Promise<SiteSettings> {
  try {
    return await getPublicSiteSettings();
  } catch {
    return fallbackSiteSettings;
  }
}
