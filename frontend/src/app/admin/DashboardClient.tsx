"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { AdminButton } from "@/components/AdminButton";
import { AdminChrome } from "@/components/AdminChrome";
import { ApiError } from "@/lib/api/client";
import { createDraft, getAdminDashboard } from "@/lib/api/posts";
import type { AdminDashboard as DashboardData, AdminDashboardPost } from "@/lib/api/types";

const statusLabel: Record<AdminDashboardPost["status"], string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  UNPUBLISHED: "已下线",
  TRASHED: "回收站",
};

function formatDateTime(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function AdminDashboard() {
  const router = useRouter();
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getAdminDashboard()
      .then((dashboard) => {
        if (!cancelled) setData(dashboard);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setError(err instanceof ApiError ? err.message : "加载控制台失败");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function onCreateDraft() {
    setCreating(true);
    try {
      const draft = await createDraft(
        `未命名草稿 ${new Date().toLocaleString("zh-CN")}`,
        "# 新文章\n\n开始写作吧。\n"
      );
      router.push(`/admin/posts/${draft.id}`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      setError(err instanceof ApiError ? err.message : "创建草稿失败");
    } finally {
      setCreating(false);
    }
  }

  const counts = data?.counts;

  return (
    <div className="mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6">
      <AdminChrome title="控制台">
        <AdminButton href="/admin/settings">进入设置</AdminButton>
        <AdminButton
          type="button"
          variant="primary"
          disabled={creating}
          onClick={() => void onCreateDraft()}
        >
          {creating ? "创建中…" : "新建文章"}
        </AdminButton>
      </AdminChrome>

      {error ? (
        <p className="mb-6 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}

      {loading || !counts ? (
        <p className="text-sm text-mist">加载中…</p>
      ) : (
        <>
          <section className="grid grid-cols-2 gap-px overflow-hidden rounded-xl border border-line bg-line sm:grid-cols-4">
            <StatCard label="文章" value={counts.total} href="/admin/posts" />
            <StatCard label="已发布" value={counts.published} href="/admin/posts" />
            <StatCard label="草稿" value={counts.draft} href="/admin/posts" />
            <StatCard label="已下线" value={counts.unpublished} href="/admin/posts" />
          </section>
          <p className="mt-3 text-sm text-mist">
            回收站 {counts.trashed} 篇
            <span className="mx-2 text-gold/70" aria-hidden>
              ·
            </span>
            已发布累计阅读 {counts.publishedViewCount} 次
          </p>

          <div className="mt-10 grid gap-10 sm:grid-cols-2">
            <RecentList
              title="最近编辑"
              emptyText="还没有编辑过的文章。"
              posts={data.recentlyEdited}
              timeOf={(post) => post.updatedAt}
            />
            <RecentList
              title="最近发布"
              emptyText="还没有已发布文章。"
              posts={data.recentlyPublished}
              timeOf={(post) => post.publishedAt}
            />
          </div>
        </>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  href,
}: {
  label: string;
  value: number;
  href: string;
}) {
  return (
    <Link
      href={href}
      className="block bg-white/80 px-4 py-5 transition-colors hover:bg-seal-soft/40"
    >
      <p className="text-xs tracking-[0.2em] text-mist">{label}</p>
      <p className="mt-2 font-serif text-3xl tracking-wide text-ink">{value}</p>
    </Link>
  );
}

function RecentList({
  title,
  emptyText,
  posts,
  timeOf,
}: {
  title: string;
  emptyText: string;
  posts: AdminDashboardPost[];
  timeOf: (post: AdminDashboardPost) => string | null;
}) {
  return (
    <section>
      <div className="mb-4 flex items-baseline justify-between gap-3">
        <h2 className="font-serif text-xl tracking-[0.12em] text-ink">{title}</h2>
        <Link href="/admin/posts" className="text-sm text-mist hover:text-seal">
          全部
        </Link>
      </div>
      {posts.length === 0 ? (
        <p className="border-y border-line py-8 text-sm text-mist">{emptyText}</p>
      ) : (
        <ul className="divide-y divide-line border-y border-line">
          {posts.map((post) => (
            <li key={post.id} className="py-4">
              <Link
                href={`/admin/posts/${post.id}`}
                className="block min-w-0 cursor-pointer transition-colors hover:text-seal"
              >
                <p className="truncate font-serif text-lg tracking-wide text-ink">
                  {post.title}
                </p>
                <p className="mt-1 text-sm text-mist">
                  {statusLabel[post.status]}
                  <span className="mx-2 text-gold/70" aria-hidden>
                    ·
                  </span>
                  {formatDateTime(timeOf(post))}
                  {post.status === "PUBLISHED" ? (
                    <>
                      <span className="mx-2 text-gold/70" aria-hidden>
                        ·
                      </span>
                      {post.viewCount} 次阅读
                    </>
                  ) : null}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
