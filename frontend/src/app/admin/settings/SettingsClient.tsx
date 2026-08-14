"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { AdminChrome } from "@/components/AdminChrome";
import { ApiError } from "@/lib/api/client";
import { getAdminSiteSettings, updateSiteSettings } from "@/lib/api/posts";
import { fallbackSiteSettings } from "@/lib/site-settings";
import type { SiteSettings } from "@/lib/api/types";

type FormState = Omit<SiteSettings, "aboutHtml" | "updatedAt">;

function toForm(settings: SiteSettings): FormState {
  return {
    siteName: settings.siteName,
    siteSubtitle: settings.siteSubtitle,
    siteDescription: settings.siteDescription,
    authorName: settings.authorName,
    authorAvatarUrl: settings.authorAvatarUrl,
    aboutMarkdown: settings.aboutMarkdown,
    postsPerPage: settings.postsPerPage,
    timezone: settings.timezone,
    defaultLanguage: settings.defaultLanguage,
    faviconUrl: settings.faviconUrl,
    defaultShareImageUrl: settings.defaultShareImageUrl,
  };
}

export function AdminSiteSettings() {
  const router = useRouter();
  const [form, setForm] = useState<FormState>(toForm(fallbackSiteSettings));
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getAdminSiteSettings()
      .then((settings) => {
        if (!cancelled) setForm(toForm(settings));
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setError(err instanceof ApiError ? err.message : "加载失败");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  function patch<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const saved = await updateSiteSettings(form);
      setForm(toForm(saved));
      setMessage("已保存站点设置");
      router.refresh();
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      setError(err instanceof ApiError ? err.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  const fieldClass =
    "min-h-10 w-full rounded-md border border-line bg-white/70 px-3 text-sm text-ink outline-none focus:border-seal";

  return (
    <div className="mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6">
      <AdminChrome title="站点设置" />

      {error ? (
        <p className="mb-6 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}
      {message ? (
        <p className="mb-6 rounded-xl border border-line bg-white/70 px-4 py-3 text-sm text-mist">{message}</p>
      ) : null}

      {loading ? (
        <p className="text-sm text-mist">加载中…</p>
      ) : (
        <form className="grid gap-5" onSubmit={(event) => void onSubmit(event)}>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">站点名称</span>
            <input
              value={form.siteName}
              onChange={(event) => patch("siteName", event.target.value)}
              className={fieldClass}
              required
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">副标题</span>
            <input
              value={form.siteSubtitle}
              onChange={(event) => patch("siteSubtitle", event.target.value)}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">站点简介</span>
            <textarea
              value={form.siteDescription}
              onChange={(event) => patch("siteDescription", event.target.value)}
              className={`${fieldClass} min-h-24 py-2`}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">作者名称</span>
            <input
              value={form.authorName}
              onChange={(event) => patch("authorName", event.target.value)}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">作者头像 URL</span>
            <input
              value={form.authorAvatarUrl}
              onChange={(event) => patch("authorAvatarUrl", event.target.value)}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">关于页（Markdown）</span>
            <textarea
              value={form.aboutMarkdown}
              onChange={(event) => patch("aboutMarkdown", event.target.value)}
              className={`${fieldClass} min-h-40 py-2 font-mono text-[13px]`}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">首页每页文章数</span>
            <input
              type="number"
              min={1}
              max={100}
              value={form.postsPerPage}
              onChange={(event) => patch("postsPerPage", Number(event.target.value))}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">时区</span>
            <input
              value={form.timezone}
              onChange={(event) => patch("timezone", event.target.value)}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">默认语言</span>
            <input
              value={form.defaultLanguage}
              onChange={(event) => patch("defaultLanguage", event.target.value)}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">Favicon URL</span>
            <input
              value={form.faviconUrl}
              onChange={(event) => patch("faviconUrl", event.target.value)}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">默认分享图 URL</span>
            <input
              value={form.defaultShareImageUrl}
              onChange={(event) => patch("defaultShareImageUrl", event.target.value)}
              className={fieldClass}
            />
          </label>
          <div>
            <AdminButton type="submit" variant="primary" disabled={saving}>
              {saving ? "保存中…" : "保存"}
            </AdminButton>
          </div>
        </form>
      )}
    </div>
  );
}
