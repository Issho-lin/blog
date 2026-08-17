import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { SiteFooter, SiteHeader } from "@/components/SiteChrome";
import { ArticleView } from "@/components/ArticleView";
import { ApiError } from "@/lib/api/client";
import { getPublishedPost } from "@/lib/api/posts";
import { loadPublicSiteSettings } from "@/lib/site-settings";

type PageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  try {
    const post = await getPublishedPost(slug);
    return {
      title: post.seoTitle ?? post.title,
      description: post.seoDescription ?? post.summary ?? undefined,
      openGraph: {
        title: post.seoTitle ?? post.title,
        description: post.seoDescription ?? post.summary ?? undefined,
        type: "article",
        images: post.coverUrl ? [{ url: post.coverUrl }] : undefined,
      },
    };
  } catch {
    return { title: "文章未找到" };
  }
}

export default async function PostDetailPage({ params }: PageProps) {
  const { slug } = await params;
  const settings = await loadPublicSiteSettings();

  try {
    const post = await getPublishedPost(slug);

    return (
      <div className="flex min-h-full flex-col">
        <SiteHeader siteName={settings.siteName} />
        <ArticleView
          post={post}
          backHref="/"
          backLabel="← 返回文章列表"
        />
        <SiteFooter siteName={settings.siteName} />
      </div>
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
