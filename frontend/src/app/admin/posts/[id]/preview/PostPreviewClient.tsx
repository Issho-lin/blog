"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { ArticleView } from "@/components/ArticleView";
import { SiteFooter, SiteHeader } from "@/components/SiteChrome";
import { ApiError } from "@/lib/api/client";
import {
  getAdminPostPreview,
  getPublicSiteSettings,
  publishPost,
} from "@/lib/api/posts";
import { fallbackSiteSettings } from "@/lib/site-settings";
import type { AdminPostPreview, SiteSettings } from "@/lib/api/types";

const statusHint: Record<AdminPostPreview["status"], string> = {
  DRAFT: "预览草稿，不会公开，也不计入阅读数。",
  PUBLISHED: "预览已保存内容，与公开页样式相同，不计入阅读数。",
  UNPUBLISHED: "预览已下线稿，访客看不到，不计入阅读数。",
  TRASHED: "预览回收站中的文章，不会恢复公开状态。",
};

export function AdminPostPreviewView({ postId }: { postId: string }) {
  const router = useRouter();
  const [settings, setSettings] = useState<SiteSettings>(fallbackSiteSettings);
  const [post, setPost] = useState<AdminPostPreview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    Promise.all([getAdminPostPreview(postId), getPublicSiteSettings().catch(() => fallbackSiteSettings)])
      .then(([preview, site]) => {
        if (cancelled) return;
        setPost(preview);
        setSettings(site);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setError(err instanceof ApiError ? err.message : "加载预览失败");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [postId, router]);

  async function onPublish() {
    if (!post || post.status === "TRASHED") return;
    setPublishing(true);
    setError(null);
    try {
      await publishPost(post.id);
      const preview = await getAdminPostPreview(postId);
      setPost(preview);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "发布失败");
    } finally {
      setPublishing(false);
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-full items-center justify-center px-5 text-sm text-mist">
        加载预览…
      </div>
    );
  }

  if (error && !post) {
    return (
      <div className="mx-auto max-w-3xl px-5 py-16 sm:px-6">
        <p className="text-warn">{error}</p>
        <AdminButton href={`/admin/posts/${postId}`} className="mt-6">
          返回编辑
        </AdminButton>
      </div>
    );
  }

  if (!post) return null;

  return (
    <div className="flex min-h-full flex-col">
      <div className="print-hide border-b border-line bg-white/70">
        <div className="mx-auto flex w-full max-w-5xl flex-wrap items-center justify-between gap-3 px-5 py-3 sm:px-6">
          <p className="text-sm text-mist">{statusHint[post.status]}</p>
          <div className="flex flex-wrap gap-2">
            <AdminButton href={`/admin/posts/${postId}`}>返回编辑</AdminButton>
            {post.status === "PUBLISHED" ? (
              <AdminButton href={`/posts/${post.slug}`}>查看公开页</AdminButton>
            ) : null}
            {post.status !== "TRASHED" ? (
              <AdminButton
                type="button"
                variant="primary"
                disabled={publishing}
                onClick={() => void onPublish()}
              >
                {publishing
                  ? "发布中…"
                  : post.status === "PUBLISHED"
                    ? "更新发布"
                    : "发布"}
              </AdminButton>
            ) : null}
          </div>
        </div>
      </div>
      {error ? (
        <p className="print-hide mx-auto w-full max-w-5xl px-5 py-3 text-sm text-warn sm:px-6">
          {error}
        </p>
      ) : null}
      <SiteHeader siteName={settings.siteName} />
      <ArticleView
        post={post}
        backHref={`/admin/posts/${postId}`}
        backLabel="← 返回编辑"
        showViewCount={false}
        showPrint={false}
      />
      <SiteFooter siteName={settings.siteName} />
    </div>
  );
}
