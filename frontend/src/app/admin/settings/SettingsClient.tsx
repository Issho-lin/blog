"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { AdminCheckbox } from "@/components/AdminCheckbox";
import { AdminChrome } from "@/components/AdminChrome";
import { AdminInput, AdminTextarea } from "@/components/AdminField";
import { ApiError } from "@/lib/api/client";
import {
  getAdminSiteSettings,
  updateSiteSettings,
  changePassword,
  getAdminAiSettings,
  updateAdminAiSettings,
  rebuildAiIndex,
} from "@/lib/api/posts";
import { fallbackSiteSettings } from "@/lib/site-settings";
import type { AiSettings, SiteSettings } from "@/lib/api/types";

const SETTINGS_TABS = [
  { id: "site", label: "站点", href: "/admin/settings" },
  { id: "ai", label: "AI 模型", href: "/admin/settings?tab=ai" },
  { id: "password", label: "密码", href: "/admin/settings?tab=password" },
] as const;

type SettingsTab = (typeof SETTINGS_TABS)[number]["id"];

function resolveTab(value: string | null): SettingsTab {
  if (value === "ai" || value === "password") return value;
  return "site";
}

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
  const searchParams = useSearchParams();
  const tab = resolveTab(searchParams.get("tab"));

  return (
    <div className="mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6">
      <AdminChrome title="站点设置" />

      <nav className="mb-8 flex gap-1 overflow-x-auto border-b border-line text-sm" aria-label="设置分类">
        {SETTINGS_TABS.map((item) => {
          const active = tab === item.id;
          return (
            <Link
              key={item.id}
              href={item.href}
              scroll={false}
              aria-current={active ? "page" : undefined}
              className={`admin-tab shrink-0${active ? " is-active" : ""}`}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>

      {tab === "site" ? <SiteSettingsSection /> : null}
      {tab === "ai" ? <AiSettingsSection /> : null}
      {tab === "password" ? <PasswordSection /> : null}
    </div>
  );
}

function SiteSettingsSection() {
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

  return (
    <section>
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
            <AdminInput
              value={form.siteName}
              onChange={(event) => patch("siteName", event.target.value)}
              required
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">副标题</span>
            <AdminInput
              value={form.siteSubtitle}
              onChange={(event) => patch("siteSubtitle", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">站点简介</span>
            <AdminTextarea
              value={form.siteDescription}
              onChange={(event) => patch("siteDescription", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">作者名称</span>
            <AdminInput
              value={form.authorName}
              onChange={(event) => patch("authorName", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">作者头像 URL</span>
            <AdminInput
              value={form.authorAvatarUrl}
              onChange={(event) => patch("authorAvatarUrl", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">关于页（Markdown）</span>
            <AdminTextarea
              value={form.aboutMarkdown}
              onChange={(event) => patch("aboutMarkdown", event.target.value)}
              className="min-h-40 font-mono text-[13px]"
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">首页每页文章数</span>
            <AdminInput
              type="number"
              min={1}
              max={100}
              value={form.postsPerPage}
              onChange={(event) => patch("postsPerPage", Number(event.target.value))}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">时区</span>
            <AdminInput
              value={form.timezone}
              onChange={(event) => patch("timezone", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">默认语言</span>
            <AdminInput
              value={form.defaultLanguage}
              onChange={(event) => patch("defaultLanguage", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">Favicon URL</span>
            <AdminInput
              value={form.faviconUrl}
              onChange={(event) => patch("faviconUrl", event.target.value)}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">默认分享图 URL</span>
            <AdminInput
              value={form.defaultShareImageUrl}
              onChange={(event) => patch("defaultShareImageUrl", event.target.value)}
            />
          </label>
          <div>
            <AdminButton type="submit" variant="primary" disabled={saving}>
              {saving ? "保存中…" : "保存"}
            </AdminButton>
          </div>
        </form>
      )}
    </section>
  );
}

function AiSettingsSection() {
  const router = useRouter();
  const [form, setForm] = useState({
    enabled: false,
    assistantEnabled: false,
    chatBaseUrl: "",
    chatApiKey: "",
    chatModel: "",
    embedBaseUrl: "",
    embedApiKey: "",
    embedModel: "",
    embedDimensions: 1536,
    assistantPersona: "",
    ratePerMinute: 10,
    ratePerDay: 50,
  });
  const [chatKeySet, setChatKeySet] = useState(false);
  const [embedKeySet, setEmbedKeySet] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [rebuilding, setRebuilding] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getAdminAiSettings()
      .then((settings: AiSettings) => {
        if (cancelled) return;
        setForm({
          enabled: settings.enabled,
          assistantEnabled: settings.assistantEnabled,
          chatBaseUrl: settings.chatBaseUrl,
          chatApiKey: "",
          chatModel: settings.chatModel,
          embedBaseUrl: settings.embedBaseUrl,
          embedApiKey: "",
          embedModel: settings.embedModel,
          embedDimensions: settings.embedDimensions,
          assistantPersona: settings.assistantPersona,
          ratePerMinute: settings.ratePerMinute,
          ratePerDay: settings.ratePerDay,
        });
        setChatKeySet(settings.chatApiKeyConfigured);
        setEmbedKeySet(settings.embedApiKeyConfigured);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setError(err instanceof ApiError ? err.message : "加载 AI 设置失败");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const saved = await updateAdminAiSettings(form);
      setChatKeySet(saved.chatApiKeyConfigured);
      setEmbedKeySet(saved.embedApiKeyConfigured);
      setForm((current) => ({ ...current, chatApiKey: "", embedApiKey: "" }));
      setMessage("已保存 AI 设置，立即生效");
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

  async function onRebuild() {
    setRebuilding(true);
    setError(null);
    setMessage(null);
    try {
      const result = await rebuildAiIndex();
      setMessage(`已回填 ${result.indexed} 篇已发布文章到知识库`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "重建索引失败");
    } finally {
      setRebuilding(false);
    }
  }

  return (
    <section>
      <p className="text-sm text-mist">
        在这里切换对话模型和向量模型。密钥只保存在服务器，页面不会回显。留空密钥表示不修改已保存的值。
      </p>
      {error ? (
        <p className="mt-4 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}
      {message ? (
        <p className="mt-4 rounded-xl border border-line bg-white/70 px-4 py-3 text-sm text-mist">
          {message}
        </p>
      ) : null}
      {loading ? (
        <p className="mt-6 text-sm text-mist">加载中…</p>
      ) : (
        <form className="mt-6 grid gap-5" onSubmit={(event) => void onSubmit(event)}>
          <label className="flex items-center gap-3 text-sm text-ink">
            <AdminCheckbox
              checked={form.enabled}
              onCheckedChange={(checked) => setForm((current) => ({ ...current, enabled: checked }))}
              aria-label="启用 AI"
            />
            启用 AI（摘要、帮写、语料同步）
          </label>
          <label className="flex items-center gap-3 text-sm text-ink">
            <AdminCheckbox
              checked={form.assistantEnabled}
              onCheckedChange={(checked) =>
                setForm((current) => ({ ...current, assistantEnabled: checked }))
              }
              aria-label="公开助手"
            />
            公开站显示全局助手
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">对话接口 Base URL</span>
            <AdminInput
              value={form.chatBaseUrl}
              onChange={(event) => setForm((current) => ({ ...current, chatBaseUrl: event.target.value }))}
              placeholder="https://api.deepseek.com"
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">对话模型</span>
            <AdminInput
              value={form.chatModel}
              onChange={(event) => setForm((current) => ({ ...current, chatModel: event.target.value }))}
              placeholder="deepseek-chat"
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">对话 API Key{chatKeySet ? "（已配置，留空则不改）" : ""}</span>
            <AdminInput
              type="password"
              autoComplete="off"
              value={form.chatApiKey}
              onChange={(event) => setForm((current) => ({ ...current, chatApiKey: event.target.value }))}
              placeholder={chatKeySet ? "••••••••" : "sk-..."}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">向量接口 Base URL</span>
            <AdminInput
              value={form.embedBaseUrl}
              onChange={(event) => setForm((current) => ({ ...current, embedBaseUrl: event.target.value }))}
              placeholder="留空则与对话接口相同"
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">向量模型</span>
            <AdminInput
              value={form.embedModel}
              onChange={(event) => setForm((current) => ({ ...current, embedModel: event.target.value }))}
              placeholder="text-embedding-3-small"
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">向量 API Key{embedKeySet ? "（已配置，留空则不改）" : ""}</span>
            <AdminInput
              type="password"
              autoComplete="off"
              value={form.embedApiKey}
              onChange={(event) => setForm((current) => ({ ...current, embedApiKey: event.target.value }))}
              placeholder={embedKeySet ? "••••••••" : "留空则与对话密钥相同"}
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">向量维度</span>
            <AdminInput
              type="number"
              min={8}
              max={4096}
              value={form.embedDimensions}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  embedDimensions: Number(event.target.value) || 1536,
                }))
              }
            />
          </label>
          <label className="grid gap-1 text-sm">
            <span className="text-mist">助手人设（可选）</span>
            <AdminTextarea
              value={form.assistantPersona}
              onChange={(event) =>
                setForm((current) => ({ ...current, assistantPersona: event.target.value }))
              }
              rows={3}
              placeholder="例如：用简洁的中文回答，偏工程实践"
            />
          </label>
          <div className="grid gap-5 sm:grid-cols-2">
            <label className="grid gap-1 text-sm">
              <span className="text-mist">公开助手每分钟上限</span>
              <AdminInput
                type="number"
                min={1}
                max={120}
                value={form.ratePerMinute}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    ratePerMinute: Number(event.target.value) || 1,
                  }))
                }
              />
            </label>
            <label className="grid gap-1 text-sm">
              <span className="text-mist">公开助手每天上限</span>
              <AdminInput
                type="number"
                min={1}
                max={2000}
                value={form.ratePerDay}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    ratePerDay: Number(event.target.value) || 1,
                  }))
                }
              />
            </label>
          </div>
          <p className="text-xs text-mist">
            DeepSeek 等只有对话能力的接口需要另配 embedding。更换向量模型或维度后请点「回填已发布文章」。
          </p>
          <div className="flex flex-wrap gap-2">
            <AdminButton type="submit" variant="primary" disabled={saving}>
              {saving ? "保存中…" : "保存 AI 设置"}
            </AdminButton>
            <AdminButton type="button" disabled={rebuilding || !form.enabled} onClick={() => void onRebuild()}>
              {rebuilding ? "回填中…" : "回填已发布文章"}
            </AdminButton>
          </div>
        </form>
      )}
    </section>
  );
}

function PasswordSection() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (newPassword !== confirmPassword) {
      setError("两次输入的新密码不一致");
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const result = await changePassword(currentPassword, newPassword);
      setMessage(result.message);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "修改失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <p className="text-sm text-mist">登录后在此更换密码。忘记密码请从登录页申请重置。</p>
      {error ? (
        <p className="mt-4 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}
      {message ? (
        <p className="mt-4 rounded-xl border border-line bg-white/70 px-4 py-3 text-sm text-mist">
          {message}
        </p>
      ) : null}
      <form className="mt-6 grid max-w-md gap-5" onSubmit={(event) => void onSubmit(event)}>
        <label className="grid gap-1 text-sm">
          <span className="text-mist">当前密码</span>
          <AdminInput
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            required
          />
        </label>
        <label className="grid gap-1 text-sm">
          <span className="text-mist">新密码</span>
          <AdminInput
            type="password"
            autoComplete="new-password"
            minLength={8}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            required
          />
        </label>
        <label className="grid gap-1 text-sm">
          <span className="text-mist">确认新密码</span>
          <AdminInput
            type="password"
            autoComplete="new-password"
            minLength={8}
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            required
          />
        </label>
        <div>
          <AdminButton type="submit" variant="primary" disabled={saving}>
            {saving ? "更新中…" : "更新密码"}
          </AdminButton>
        </div>
      </form>
    </section>
  );
}

