"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { AdminButton } from "@/components/AdminButton";
import { AdminChrome } from "@/components/AdminChrome";
import { AdminConfirmDialog } from "@/components/AdminConfirmDialog";
import { TaxonomyRow } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import {
  createDraft,
  exportPost,
  importMarkdown,
  listAdminPosts,
  listCategories,
  listTags,
  permanentlyDeletePost,
  publishPost,
  restorePost,
  trashPost,
  unpublishPost,
} from "@/lib/api/posts";
import type { AdminPost, Category, Tag } from "@/lib/api/types";

const statusFilters = [
  { id: "ALL", label: "全部" },
  { id: "DRAFT", label: "草稿" },
  { id: "PUBLISHED", label: "已发布" },
  { id: "UNPUBLISHED", label: "已下线" },
  { id: "TRASHED", label: "回收站" },
] as const;

type StatusFilter = (typeof statusFilters)[number]["id"];

export function AdminPostsList() {
  const router = useRouter();
  const fileRef = useRef<HTMLInputElement>(null);
  const [posts, setPosts] = useState<AdminPost[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [actingId, setActingId] = useState<string | null>(null);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [deleteConfirm, setDeleteConfirm] = useState<AdminPost | null>(null);

  async function loadPosts() {
    setLoading(true);
    setError(null);
    try {
      const [data, categoryList, tagList] = await Promise.all([
        listAdminPosts(keyword.trim() || undefined),
        listCategories().catch(() => [] as Category[]),
        listTags().catch(() => [] as Tag[]),
      ]);
      setPosts(data);
      setCategories(categoryList);
      setTags(tagList);
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

  function patchPost(updated: AdminPost) {
    setPosts((current) =>
      current.map((post) => (post.id === updated.id ? updated : post))
    );
  }

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

  async function onImport(file: File) {
    setImporting(true);
    setError(null);
    try {
      const result = await importMarkdown(file);
      router.push(`/admin/posts/${result.id}`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      setError(err instanceof ApiError ? err.message : "导入失败");
    } finally {
      setImporting(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  }

  async function onExport(post: AdminPost) {
    setActingId(post.id);
    setError(null);
    try {
      await exportPost(post.id, `${post.slug || "post"}.md`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "导出失败");
    } finally {
      setActingId(null);
    }
  }

  async function onPublish(postId: string) {
    setActingId(postId);
    try {
      const updated = await publishPost(postId);
      patchPost(updated);
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
      patchPost(updated);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "下线失败");
    } finally {
      setActingId(null);
    }
  }

  async function onTrash(post: AdminPost) {
    if (!window.confirm(`把「${post.title}」移入回收站？`)) return;
    setActingId(post.id);
    try {
      patchPost(await trashPost(post.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "移入回收站失败");
    } finally {
      setActingId(null);
    }
  }

  async function onRestore(postId: string) {
    setActingId(postId);
    try {
      patchPost(await restorePost(postId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "恢复失败");
    } finally {
      setActingId(null);
    }
  }

  function requestPermanentlyDelete(post: AdminPost) {
    setDeleteConfirm(post);
  }

  function onDeleteConfirm() {
    if (!deleteConfirm) return;
    const post = deleteConfirm;
    setDeleteConfirm(null);
    void onPermanentlyDelete(post);
  }

  async function onPermanentlyDelete(post: AdminPost) {
    setActingId(post.id);
    try {
      await permanentlyDeletePost(post.id);
      setPosts((current) => current.filter((item) => item.id !== post.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "彻底删除失败");
    } finally {
      setActingId(null);
    }
  }

  const visiblePosts =
    statusFilter === "ALL"
      ? posts
      : posts.filter((post) => post.status === statusFilter);

  const emptyText =
    posts.length === 0
      ? "还没有文章。点右上角「新建草稿」或导入一份 Markdown。"
      : "这个筛选条件下没有文章。";

  const counts = {
    ALL: posts.length,
    DRAFT: posts.filter((post) => post.status === "DRAFT").length,
    PUBLISHED: posts.filter((post) => post.status === "PUBLISHED").length,
    UNPUBLISHED: posts.filter((post) => post.status === "UNPUBLISHED").length,
    TRASHED: posts.filter((post) => post.status === "TRASHED").length,
  };

  return (
    <div className="mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6">
      <AdminChrome title="文章">
        <input
          ref={fileRef}
          type="file"
          accept=".md,.markdown,text/markdown"
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) void onImport(file);
          }}
        />
        <AdminButton
          type="button"
          disabled={importing}
          onClick={() => fileRef.current?.click()}
        >
          {importing ? "导入中…" : "导入 Markdown"}
        </AdminButton>
        <AdminButton
          type="button"
          variant="primary"
          disabled={creating}
          onClick={onCreateDraft}
        >
          {creating ? "创建中…" : "新建草稿"}
        </AdminButton>
      </AdminChrome>

      {error ? (
        <div
          role="alert"
          className="mb-6 flex items-start justify-between gap-3 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn"
        >
          <p>{error}</p>
          <button
            type="button"
            onClick={() => setError(null)}
            className="mt-0.5 shrink-0 cursor-pointer p-0.5 text-warn/80 transition-colors hover:text-warn"
            aria-label="关闭提示"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 16 16"
              width="14"
              height="14"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.6"
              strokeLinecap="round"
              aria-hidden="true"
            >
              <path d="M4 4l8 8M12 4l-8 8" />
            </svg>
          </button>
        </div>
      ) : null}

      <form
        className="mb-5 flex flex-wrap gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          void loadPosts();
        }}
      >
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="按标题搜索"
          className="min-h-10 min-w-[12rem] flex-1 rounded-md border border-line bg-white/70 px-3 text-sm text-ink outline-none focus:border-seal"
        />
        <AdminButton type="submit">搜索</AdminButton>
      </form>

      <nav className="mb-6 flex flex-wrap gap-1 text-sm" aria-label="按状态筛选">
        {statusFilters.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`admin-tab${statusFilter === item.id ? " is-active" : ""}`}
            onClick={() => setStatusFilter(item.id)}
          >
            {item.label}
            <span className="ml-1 text-xs text-mist">{counts[item.id]}</span>
          </button>
        ))}
      </nav>

      {loading ? (
        <p className="text-sm text-mist">加载中…</p>
      ) : visiblePosts.length === 0 ? (
        <div className="border-y border-line py-12 text-sm text-mist">{emptyText}</div>
      ) : (
        <ul className="divide-y divide-line border-y border-line">
          {visiblePosts.map((post) => (
            <li
              key={post.id}
              className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2 text-sm text-mist">
                  <span className="text-seal">
                    {statusFilters.find((item) => item.id === post.status)?.label}
                  </span>
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
                <div className="mt-2">
                  <TaxonomyRow
                    categoryName={
                      categories.find((item) => item.id === post.categoryId)?.name
                    }
                    tags={(post.tagIds ?? [])
                      .map((id) => tags.find((item) => item.id === id)?.name)
                      .filter((name): name is string => Boolean(name))
                      .map((name) => ({ name }))}
                  />
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {post.status === "TRASHED" ? (
                  <>
                    <AdminButton
                      type="button"
                      variant="soft"
                      disabled={actingId === post.id}
                      onClick={() => void onRestore(post.id)}
                    >
                      恢复
                    </AdminButton>
                    <AdminButton
                      type="button"
                      variant="danger"
                      disabled={actingId === post.id}
                      onClick={() => requestPermanentlyDelete(post)}
                    >
                      彻底删除
                    </AdminButton>
                  </>
                ) : (
                  <>
                    <AdminButton href={`/admin/posts/${post.id}`}>编辑</AdminButton>
                    <AdminButton
                      type="button"
                      disabled={actingId === post.id}
                      onClick={() => void onExport(post)}
                    >
                      导出
                    </AdminButton>
                    {post.status === "PUBLISHED" ? (
                      <>
                        <AdminButton href={`/posts/${post.slug}`}>查看</AdminButton>
                        <AdminButton
                          type="button"
                          disabled={actingId === post.id}
                          onClick={() => void onUnpublish(post.id)}
                        >
                          下线
                        </AdminButton>
                      </>
                    ) : null}
                    {post.status === "DRAFT" || post.status === "UNPUBLISHED" ? (
                      <>
                        <AdminButton
                          type="button"
                          variant="soft"
                          disabled={actingId === post.id}
                          onClick={() => void onPublish(post.id)}
                        >
                          发布
                        </AdminButton>
                        <AdminButton
                          type="button"
                          variant="danger"
                          disabled={actingId === post.id}
                          onClick={() => void onTrash(post)}
                        >
                          回收站
                        </AdminButton>
                      </>
                    ) : null}
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
      {deleteConfirm ? (
        <AdminConfirmDialog
          title="彻底删除"
          message={`彻底删除「${deleteConfirm.title}」？此操作不可恢复。`}
          confirmLabel="确认彻底删除"
          onCancel={() => setDeleteConfirm(null)}
          onConfirm={onDeleteConfirm}
        />
      ) : null}
    </div>
  );
}
