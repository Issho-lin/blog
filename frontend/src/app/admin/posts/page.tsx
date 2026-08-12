"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { SealMark } from "@/components/SealMark";
import { ApiError } from "@/lib/api/client";
import {
  createDraft,
  listAdminPosts,
  logout,
  publishPost,
  unpublishPost,
} from "@/lib/api/posts";
import type { AdminPost } from "@/lib/api/types";

const statusLabel: Record<AdminPost["status"], string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  UNPUBLISHED: "已下线",
  TRASHED: "回收站",
};

export default function AdminPostsPage() {
  const router = useRouter();
  const [posts, setPosts] = useState<AdminPost[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [actingId, setActingId] = useState<string | null>(null);

  async function loadPosts() {
    setLoading(true);
    setError(null);
    try {
      const data = await listAdminPosts();
      setPosts(data);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      setError(err instanceof ApiError ? err.message : "加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadPosts();
  }, []);

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

  async function onPublish(postId: string) {
    setActingId(postId);
    try {
      const updated = await publishPost(postId);
      setPosts((current) =>
        current.map((post) => (post.id === postId ? updated : post))
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "发布失败");
    } finally {
      setActingId(null);
    }
  }

  async function onUnpublish(postId: string) {
    setActingId(postId);
    try {
      const updated = await unpublishPost(postId);
      setPosts((current) =>
        current.map((post) => (post.id === postId ? updated : post))
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "下线失败");
    } finally {
      setActingId(null);
    }
  }

  async function onLogout() {
    await logout().catch(() => undefined);
    router.replace("/admin/login");
  }

  return (
    <div className="mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6">
      <div className="mb-10 flex flex-wrap items-end justify-between gap-4 border-b border-line pb-8">
        <div className="flex items-center gap-3">
          <SealMark size={34} className="text-seal seal-glow" />
          <div>
            <p className="text-xs tracking-[0.35em] text-gold">管理台</p>
            <h1 className="font-serif text-3xl tracking-wide text-ink">文章</h1>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            href="/"
            className="inline-flex min-h-11 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors duration-200 hover:border-ink hover:text-ink"
          >
            公开站
          </Link>
          <button
            type="button"
            onClick={onLogout}
            className="inline-flex min-h-11 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors duration-200 hover:border-ink hover:text-ink"
          >
            退出
          </button>
          <button
            type="button"
            disabled={creating}
            onClick={onCreateDraft}
            className="inline-flex min-h-11 cursor-pointer items-center rounded-full bg-ink px-4 text-sm text-paper transition-colors duration-200 hover:bg-seal disabled:cursor-not-allowed disabled:opacity-60"
          >
            {creating ? "创建中…" : "新建草稿"}
          </button>
        </div>
      </div>

      {error ? (
        <p className="mb-6 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}

      {loading ? (
        <p className="text-sm text-mist">加载中…</p>
      ) : posts.length === 0 ? (
        <div className="border-y border-line py-12 text-sm text-mist">
          还没有文章。点右上角「新建草稿」开始。
        </div>
      ) : (
        <ul className="divide-y divide-line border-y border-line">
          {posts.map((post) => (
            <li
              key={post.id}
              className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2 text-sm text-mist">
                  <span className="text-seal">{statusLabel[post.status]}</span>
                  <span className="truncate">{post.slug}</span>
                </div>
                <h2 className="mt-1 truncate font-serif text-xl tracking-wide text-ink">
                  <Link
                    href={`/admin/posts/${post.id}`}
                    className="cursor-pointer transition-colors hover:text-seal"
                  >
                    {post.title}
                  </Link>
                </h2>
              </div>
              <div className="flex flex-wrap gap-2">
                <Link
                  href={`/admin/posts/${post.id}`}
                  className="inline-flex min-h-11 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors duration-200 hover:border-ink hover:text-ink"
                >
                  编辑
                </Link>
                {post.status === "PUBLISHED" ? (
                  <>
                    <Link
                      href={`/posts/${post.slug}`}
                      className="inline-flex min-h-11 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors duration-200 hover:border-seal hover:text-seal"
                    >
                      查看
                    </Link>
                    <button
                      type="button"
                      disabled={actingId === post.id}
                      onClick={() => void onUnpublish(post.id)}
                      className="inline-flex min-h-11 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors duration-200 hover:border-seal hover:text-seal disabled:opacity-60"
                    >
                      下线
                    </button>
                  </>
                ) : null}
                {post.status === "DRAFT" || post.status === "UNPUBLISHED" ? (
                  <button
                    type="button"
                    disabled={actingId === post.id}
                    onClick={() => void onPublish(post.id)}
                    className="inline-flex min-h-11 cursor-pointer items-center rounded-full bg-seal-soft px-4 text-sm text-seal transition-colors duration-200 hover:bg-seal hover:text-paper disabled:opacity-60"
                  >
                    发布
                  </button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
