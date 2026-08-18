"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { AdminButton } from "@/components/AdminButton";
import { AdminCheckbox } from "@/components/AdminCheckbox";
import { AdminChrome } from "@/components/AdminChrome";
import { AdminConfirmDialog } from "@/components/AdminConfirmDialog";
import { AdminInput } from "@/components/AdminField";
import { AdminSelect } from "@/components/AdminSelect";
import { AdminUploadProgress } from "@/components/AdminUploadProgress";
import { TaxonomyRow } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import {
  batchTrashPosts,
  batchUnpublishPosts,
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
  const [importPercent, setImportPercent] = useState<number | null>(null);
  const [actingId, setActingId] = useState<string | null>(null);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [tagFilter, setTagFilter] = useState("");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [deleteConfirm, setDeleteConfirm] = useState<AdminPost | null>(null);
  const [trashConfirm, setTrashConfirm] = useState<AdminPost | null>(null);
  const [bulkAction, setBulkAction] = useState<"unpublish" | "trash" | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [pendingImport, setPendingImport] = useState<{
    markdown: File;
    images: File[];
  } | null>(null);
  const [importTargetId, setImportTargetId] = useState("");
  const [importOverwriteOpen, setImportOverwriteOpen] = useState(false);

  async function loadPosts() {
    setLoading(true);
    setError(null);
    try {
      const [data, categoryList, tagList] = await Promise.all([
        listAdminPosts({
          keyword: keyword.trim() || undefined,
          categoryId: categoryFilter || undefined,
          tagId: tagFilter || undefined,
        }),
        listCategories().catch(() => [] as Category[]),
        listTags().catch(() => [] as Tag[]),
      ]);
      setPosts(data);
      setCategories(categoryList);
      setTags(tagList);
      setSelectedIds((current) =>
        current.filter((id) => data.some((post) => post.id === id))
      );
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
  }, [categoryFilter, tagFilter]);

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

  function queueImportFiles(files: File[]) {
    const markdowns = files.filter((file) => /\.(md|markdown)$/i.test(file.name));
    const images = files.filter((file) =>
      /\.(png|jpe?g|gif|webp)$/i.test(file.name)
    );
    if (markdowns.length === 0) {
      setError("请包含一份 .md 或 .markdown 文件");
      return;
    }
    setError(null);
    setImportTargetId("");
    setPendingImport({ markdown: markdowns[0], images });
  }

  async function runImport(targetPostId?: string) {
    if (!pendingImport) return;
    setImporting(true);
    setImportPercent(0);
    setError(null);
    try {
      const result = await importMarkdown(pendingImport.markdown, {
        images: pendingImport.images,
        targetPostId,
        confirmOverwrite: Boolean(targetPostId),
        onProgress: (percent) => setImportPercent(percent),
      });
      setPendingImport(null);
      setImportOverwriteOpen(false);
      if (result.warnings && result.warnings.length > 0) {
        setError(result.warnings.join("；"));
      }
      router.push(`/admin/posts/${result.id}`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      setError(err instanceof ApiError ? err.message : "导入失败");
    } finally {
      setImporting(false);
      setImportPercent(null);
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

  function requestTrash(post: AdminPost) {
    setTrashConfirm(post);
  }

  async function onTrash(post: AdminPost) {
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
      setSelectedIds((current) => current.filter((id) => id !== post.id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "彻底删除失败");
    } finally {
      setActingId(null);
    }
  }

  function toggleSelected(postId: string) {
    setSelectedIds((current) =>
      current.includes(postId)
        ? current.filter((id) => id !== postId)
        : [...current, postId]
    );
  }

  async function runBulkUnpublish(ids: string[]) {
    setActingId("bulk");
    try {
      const result = await batchUnpublishPosts(ids);
      result.succeeded.forEach(patchPost);
      setSelectedIds([]);
      if (result.failed.length > 0) {
        setError(
          `${result.succeeded.length} 篇已下线，${result.failed.length} 篇无法下线（通常不是已发布状态）。`
        );
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "批量下线失败");
    } finally {
      setActingId(null);
    }
  }

  async function runBulkTrash(ids: string[]) {
    setActingId("bulk");
    try {
      const result = await batchTrashPosts(ids);
      result.succeeded.forEach(patchPost);
      setSelectedIds([]);
      if (result.failed.length > 0) {
        setError(
          `${result.succeeded.length} 篇已移入回收站，${result.failed.length} 篇失败。`
        );
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "批量移入回收站失败");
    } finally {
      setActingId(null);
    }
  }

  const visiblePosts =
    statusFilter === "ALL"
      ? posts
      : posts.filter((post) => post.status === statusFilter);

  const visibleSelectedIds = selectedIds.filter((id) =>
    visiblePosts.some((post) => post.id === id)
  );
  const allVisibleSelected =
    visiblePosts.length > 0 && visibleSelectedIds.length === visiblePosts.length;

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
    <div
      className="relative mx-auto min-h-full w-full max-w-3xl px-5 py-10 sm:px-6"
      onDragOver={(event) => {
        event.preventDefault();
        setDragActive(true);
      }}
      onDragLeave={(event) => {
        if (event.currentTarget.contains(event.relatedTarget as Node)) return;
        setDragActive(false);
      }}
      onDrop={(event) => {
        event.preventDefault();
        setDragActive(false);
        queueImportFiles(Array.from(event.dataTransfer.files));
      }}
    >
      {dragActive ? (
        <div className="pointer-events-none absolute inset-3 z-20 flex items-center justify-center rounded-2xl border border-dashed border-seal bg-paper/90 text-sm text-seal">
          松开即可导入 Markdown，配图可一并拖入
        </div>
      ) : null}
      <AdminChrome title="文章">
        <input
          ref={fileRef}
          type="file"
          multiple
          accept=".md,.markdown,text/markdown,.png,.jpg,.jpeg,.gif,.webp,image/*"
          className="hidden"
          onChange={(event) => {
            const files = event.target.files;
            if (files && files.length > 0) queueImportFiles(Array.from(files));
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
        <AdminInput
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="按标题搜索"
          className="min-w-[12rem] flex-1"
        />
        <AdminSelect
          value={categoryFilter}
          onValueChange={setCategoryFilter}
          aria-label="按分类筛选"
          options={[
            { value: "", label: "全部分类" },
            ...categories.map((category) => ({
              value: category.id,
              label: category.name,
            })),
          ]}
        />
        <AdminSelect
          value={tagFilter}
          onValueChange={setTagFilter}
          aria-label="按标签筛选"
          options={[
            { value: "", label: "全部标签" },
            ...tags.map((tag) => ({
              value: tag.id,
              label: `#${tag.name}`,
            })),
          ]}
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

      {visibleSelectedIds.length > 0 ? (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-white/70 px-4 py-3 text-sm">
          <span className="text-mist">已选 {visibleSelectedIds.length} 篇</span>
          <div className="flex flex-wrap gap-2">
            <AdminButton
              type="button"
              disabled={actingId === "bulk"}
              onClick={() => setBulkAction("unpublish")}
            >
              批量下线
            </AdminButton>
            <AdminButton
              type="button"
              variant="danger"
              disabled={actingId === "bulk"}
              onClick={() => setBulkAction("trash")}
            >
              批量删除
            </AdminButton>
            <AdminButton type="button" onClick={() => setSelectedIds([])}>
              取消选择
            </AdminButton>
          </div>
        </div>
      ) : null}

      {loading ? (
        <p className="text-sm text-mist">加载中…</p>
      ) : visiblePosts.length === 0 ? (
        <div className="border-y border-line py-12 text-sm text-mist">{emptyText}</div>
      ) : (
        <ul className="divide-y divide-line border-y border-line">
          <li className="flex items-center gap-3 py-3 text-sm text-mist">
            <AdminCheckbox
              checked={
                allVisibleSelected
                  ? true
                  : visibleSelectedIds.length > 0
                    ? "indeterminate"
                    : false
              }
              onCheckedChange={() => {
                if (allVisibleSelected) {
                  setSelectedIds((current) =>
                    current.filter(
                      (id) => !visiblePosts.some((post) => post.id === id)
                    )
                  );
                } else {
                  setSelectedIds((current) => [
                    ...new Set([
                      ...current,
                      ...visiblePosts.map((post) => post.id),
                    ]),
                  ]);
                }
              }}
              aria-label="全选当前列表"
            />
            <span>全选当前列表</span>
          </li>
          {visiblePosts.map((post) => (
            <li
              key={post.id}
              className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="flex min-w-0 items-start gap-3">
                <AdminCheckbox
                  checked={selectedIds.includes(post.id)}
                  onCheckedChange={() => toggleSelected(post.id)}
                  className="mt-1.5"
                  aria-label={`选择 ${post.title}`}
                />
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
              </div>
              <div className="flex flex-wrap gap-2 sm:pl-7">
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
                    <AdminButton href={`/admin/posts/${post.id}/preview`}>预览</AdminButton>
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
                          onClick={() => requestTrash(post)}
                        >
                          删除
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
      {trashConfirm ? (
        <AdminConfirmDialog
          title="删除"
          message={`把「${trashConfirm.title}」移入回收站？`}
          confirmLabel="确认删除"
          onCancel={() => setTrashConfirm(null)}
          onConfirm={() => {
            const post = trashConfirm;
            setTrashConfirm(null);
            void onTrash(post);
          }}
        />
      ) : null}
      {deleteConfirm ? (
        <AdminConfirmDialog
          title="彻底删除"
          message={`彻底删除「${deleteConfirm.title}」？此操作不可恢复。`}
          confirmLabel="确认彻底删除"
          onCancel={() => setDeleteConfirm(null)}
          onConfirm={onDeleteConfirm}
        />
      ) : null}
      {bulkAction === "unpublish" ? (
        <AdminConfirmDialog
          title="批量下线"
          message={`将下线已选的 ${visibleSelectedIds.length} 篇文章。不是已发布状态的会被跳过。`}
          confirmLabel="确认下线"
          confirmVariant="primary"
          onCancel={() => setBulkAction(null)}
          onConfirm={() => {
            const ids = visibleSelectedIds;
            setBulkAction(null);
            void runBulkUnpublish(ids);
          }}
        />
      ) : null}
      {bulkAction === "trash" ? (
        <AdminConfirmDialog
          title="批量删除"
          message={`将把已选的 ${visibleSelectedIds.length} 篇文章移入回收站。已发布的会先下线再进入回收站。`}
          confirmLabel="确认删除"
          onCancel={() => setBulkAction(null)}
          onConfirm={() => {
            const ids = visibleSelectedIds;
            setBulkAction(null);
            void runBulkTrash(ids);
          }}
        />
      ) : null}
      {pendingImport ? (
        <div className="admin-dialog-overlay">
          <div className="admin-dialog-content">
            <h2 className="admin-dialog-title">导入 Markdown</h2>
            <p className="admin-dialog-desc">
              文件：{pendingImport.markdown.name}
              {pendingImport.images.length > 0
                ? ` · 配图 ${pendingImport.images.length} 张`
                : " · 未附带配图"}
            </p>
            <label className="mt-4 grid gap-1.5 text-sm">
              <span className="text-mist">导入到</span>
              <AdminSelect
                value={importTargetId}
                onValueChange={setImportTargetId}
                className="w-full"
                options={[
                  { value: "", label: "新建草稿" },
                  ...posts
                    .filter((post) => post.status !== "TRASHED")
                    .map((post) => ({
                      value: post.id,
                      label: `${post.title}（${statusFilters.find((item) => item.id === post.status)?.label ?? post.status}）`,
                    })),
                ]}
              />
            </label>
            {importPercent !== null ? (
              <div className="mt-4">
                <AdminUploadProgress label="上传导入文件" percent={importPercent} />
              </div>
            ) : null}
            <div className="admin-dialog-actions">
              <AdminButton
                type="button"
                onClick={() => {
                  setPendingImport(null);
                  if (fileRef.current) fileRef.current.value = "";
                }}
              >
                取消
              </AdminButton>
              <AdminButton
                type="button"
                variant="primary"
                disabled={importing}
                onClick={() => {
                  if (importTargetId) {
                    setImportOverwriteOpen(true);
                    return;
                  }
                  void runImport();
                }}
              >
                {importing ? "导入中…" : "确认导入"}
              </AdminButton>
            </div>
          </div>
        </div>
      ) : null}
      {importOverwriteOpen && pendingImport && importTargetId ? (
        <AdminConfirmDialog
          title="导入到已有文章"
          message={`将用「${pendingImport.markdown.name}」覆盖「${posts.find((post) => post.id === importTargetId)?.title ?? "所选文章"}」的正文。已发布的会先下线，不会直接公开。`}
          confirmLabel="确认覆盖"
          onCancel={() => setImportOverwriteOpen(false)}
          onConfirm={() => void runImport(importTargetId)}
        />
      ) : null}
    </div>
  );
}
