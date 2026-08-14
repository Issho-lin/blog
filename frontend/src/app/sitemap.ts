import type { MetadataRoute } from "next";
import {
  listPublicCategories,
  listPublishedPosts,
  listPublicTags,
} from "@/lib/api/posts";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [posts, categories, tags] = await Promise.all([
    listPublishedPosts(1, 200).then((page) => page.items).catch(() => []),
    listPublicCategories().catch(() => []),
    listPublicTags().catch(() => []),
  ]);

  const now = new Date();

  return [
    { url: siteUrl, lastModified: now },
    { url: `${siteUrl}/about`, lastModified: now },
    { url: `${siteUrl}/archive`, lastModified: now },
    { url: `${siteUrl}/categories`, lastModified: now },
    { url: `${siteUrl}/tags`, lastModified: now },
    { url: `${siteUrl}/search`, lastModified: now },
    ...categories.map((category) => ({
      url: `${siteUrl}/categories/${category.slug}`,
      lastModified: now,
    })),
    ...tags.map((tag) => ({
      url: `${siteUrl}/tags/${tag.slug}`,
      lastModified: now,
    })),
    ...posts.map((post) => ({
      url: `${siteUrl}/posts/${post.slug}`,
      lastModified: post.publishedAt ? new Date(post.publishedAt) : now,
    })),
  ];
}
