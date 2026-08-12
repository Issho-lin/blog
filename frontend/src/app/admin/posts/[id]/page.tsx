"use client";

import dynamic from "next/dynamic";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  useCallback,
  useEffect,
  useEffectEvent,
  useRef,
  useState,
} from "react";
import { SealMark } from "@/components/SealMark";
import { ApiError } from "@/lib/api/client";
import {
  getAdminPost,
  listCategories,
  listTags,
  publishPost,
  unpublishPost,
  updatePost,
} from "@/lib/api/posts";
import type { AdminPost, Category, Tag } from "@/lib/api/types";

const MarkdownEditor = dynamic(
  () =>
    import("@/components/MarkdownEditor").then((mod) => mod.MarkdownEditor),
  {
    ssr: false,
    loading: () => (
      <div className="flex h-[32rem] items-center justify-center rounded-xl border border-line bg-white/70 text-sm text-mist">
        正在加载编辑器…
      </div>
    ),
  }
);

type SaveState = "saved" | "dirty" | "saving" | "conflict" | "error";

const statusLabel: Record<AdminPost["status"], string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  UNPUBLISHED: "已下线",
  TRASHED: "回收站",
};

const saveStateLabel: Record<SaveState, string> = {
  saved: "已保存",
  dirty: "未保存",
  saving: "保存中…",
  conflict: "版本冲突",
  error: "保存失败",
};

export default function AdminPostEditorPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const postId = params.id;

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [markdown, setMarkdown] = useState("");
  const [editorReady, setEditorReady] = useState(false);
  const [resetToken, setResetToken] = useState(0);
  const [categoryId, setCategoryId] = useState<string>("");
  const [tagIds, setTagIds] = useState<string[]>([]);
  const [version, setVersion] = useState(0);
  const [status, setStatus] = useState<AdminPost["status"]>("DRAFT");
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [saveState, setSaveState] = useState<SaveState>("saved");
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [metaOpen, setMetaOpen] = useState(false);
  const [acting, setActing] = useState(false);

  const versionRef = useRef(0);
  const dirtyRef = useRef(false);
  const savingRef = useRef(false);
  const latestPayloadRef = useRef({
    title: "",
    slug: "",
    markdown: "",
    categoryId: "",
    tagIds: [] as string[],
  });

  const applyPost = useCallback((post: AdminPost, resetEditor = false) => {
    setTitle(post.title);
    setSlug(post.slug);
    setMarkdown(post.markdownContent ?? "");
    setCategoryId(post.categoryId ?? "");
    setTagIds(post.tagIds ?? []);
    setVersion(post.version);
    versionRef.current = post.version;
    setStatus(post.status);
    setUpdatedAt(post.updatedAt);
    setSaveState("saved");
    setSaveMessage(null);
    dirtyRef.current = false;
    if (resetEditor) {
      setResetToken((token) => token + 1);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setLoadError(null);
      try {
        const [post, categoryList, tagList] = await Promise.all([
          getAdminPost(postId),
          listCategories().catch(() => [] as Category[]),
          listTags().catch(() => [] as Tag[]),
        ]);
        if (cancelled) return;
        applyPost(post, true);
        setCategories(categoryList);
        setTags(tagList);
        setEditorReady(true);
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setLoadError(err instanceof ApiError ? err.message : "加载文章失败");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [postId, router, applyPost]);

  const markDirty = useEffectEvent(() => {
    dirtyRef.current = true;
    setSaveState("dirty");
  });

  useEffect(() => {
    latestPayloadRef.current = {
      title,
      slug,
      markdown,
      categoryId,
      tagIds,
    };
  }, [title, slug, markdown, categoryId, tagIds]);

  const persist = useEffectEvent(async (manual = false) => {
    if (savingRef.current) return;
    if (!dirtyRef.current && !manual) return;
    if (!title.trim()) {
      setSaveState("error");
      setSaveMessage("标题不能为空");
      return;
    }

    savingRef.current = true;
    setSaveState("saving");
    setSaveMessage(null);

    const payload = latestPayloadRef.current;

    try {
      const updated = await updatePost(postId, {
        title: payload.title.trim(),
        markdownContent: payload.markdown,
        slug: payload.slug.trim(),
        categoryId: payload.categoryId || null,
        tagIds: payload.tagIds,
        expectedVersion: versionRef.current,
      });
      versionRef.current = updated.version;
      setVersion(updated.version);
      setStatus(updated.status);
      setUpdatedAt(updated.updatedAt);
      setSlug(updated.slug);
      dirtyRef.current = false;
      setSaveState("saved");
      setSaveMessage(
        `已保存于 ${new Intl.DateTimeFormat("zh-CN", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        }).format(new Date(updated.updatedAt))}`
      );
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/admin/login");
        return;
      }
      if (err instanceof ApiError && err.code === "CONCURRENT_MODIFICATION") {
        setSaveState("conflict");
        setSaveMessage("其他窗口已修改本文，请重新加载后再编辑。");
        return;
      }
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "保存失败");
    } finally {
      savingRef.current = false;
    }
  });

  useEffect(() => {
    if (loading || saveState === "conflict") return;
    if (!dirtyRef.current) return;

    const timer = window.setTimeout(() => {
      void persist(false);
    }, 1000);

    return () => window.clearTimeout(timer);
  }, [title, slug, markdown, categoryId, tagIds, loading, saveState, persist]);

  useEffect(() => {
    function onBeforeUnload(event: BeforeUnloadEvent) {
      if (!dirtyRef.current) return;
      event.preventDefault();
      event.returnValue = "";
    }
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, []);

  function toggleTag(id: string) {
    markDirty();
    setTagIds((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id]
    );
  }

  async function onPublish() {
    setActing(true);
    try {
      if (dirtyRef.current) {
        await persist(true);
        if (dirtyRef.current) return;
      }
      const updated = await publishPost(postId);
      applyPost(updated, false);
      setSaveMessage("已发布");
    } catch (err) {
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "发布失败");
    } finally {
      setActing(false);
    }
  }

  async function onUnpublish() {
    setActing(true);
    try {
      const updated = await unpublishPost(postId);
      applyPost(updated, false);
      setSaveMessage("已下线");
    } catch (err) {
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "下线失败");
    } finally {
      setActing(false);
    }
  }

  async function reloadFromServer() {
    setActing(true);
    try {
      const post = await getAdminPost(postId);
      applyPost(post, true);
    } catch (err) {
      setSaveMessage(err instanceof ApiError ? err.message : "重新加载失败");
    } finally {
      setActing(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-5xl px-5 py-16 text-sm text-mist sm:px-6">
        加载编辑器…
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="mx-auto max-w-5xl px-5 py-16 sm:px-6">
        <p className="text-warn">{loadError}</p>
        <Link href="/admin/posts" className="mt-6 inline-block text-sm text-seal">
          返回列表
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto min-h-full w-full max-w-5xl px-5 py-6 sm:px-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <SealMark size={26} className="shrink-0 text-seal" />
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2 text-xs text-mist">
              <Link href="/admin/posts" className="hover:text-seal">
                文章列表
              </Link>
              <span aria-hidden>/</span>
              <span className="text-seal">{statusLabel[status]}</span>
              <span aria-hidden>·</span>
              <span>v{version}</span>
            </div>
            <p className="mt-1 text-sm text-mist">
              {saveStateLabel[saveState]}
              {saveMessage ? ` · ${saveMessage}` : null}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setMetaOpen((open) => !open)}
            className="inline-flex min-h-10 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors hover:border-ink hover:text-ink"
          >
            {metaOpen ? "收起设置" : "文章设置"}
          </button>
          <button
            type="button"
            disabled={acting || saveState === "saving"}
            onClick={() => void persist(true)}
            className="inline-flex min-h-10 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors hover:border-ink hover:text-ink disabled:opacity-60"
          >
            手动保存
          </button>
          {status === "PUBLISHED" ? (
            <button
              type="button"
              disabled={acting}
              onClick={() => void onUnpublish()}
              className="inline-flex min-h-10 cursor-pointer items-center rounded-full border border-line px-4 text-sm text-mist transition-colors hover:border-seal hover:text-seal disabled:opacity-60"
            >
              下线
            </button>
          ) : status !== "TRASHED" ? (
            <button
              type="button"
              disabled={acting}
              onClick={() => void onPublish()}
              className="inline-flex min-h-10 cursor-pointer items-center rounded-full bg-ink px-4 text-sm text-paper transition-colors hover:bg-seal disabled:opacity-60"
            >
              发布
            </button>
          ) : null}
        </div>
      </div>

      {saveState === "conflict" ? (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-warn/30 bg-seal-soft/50 px-4 py-3 text-sm text-warn">
          <span>{saveMessage}</span>
          <button
            type="button"
            onClick={() => void reloadFromServer()}
            className="cursor-pointer rounded-full bg-ink px-3 py-1.5 text-paper"
          >
            重新加载
          </button>
        </div>
      ) : null}

      <input
        value={title}
        onChange={(event) => {
          markDirty();
          setTitle(event.target.value);
        }}
        placeholder="文章标题"
        className="mb-4 w-full border-0 bg-transparent font-serif text-[clamp(1.75rem,4vw,2.4rem)] leading-snug tracking-wide text-ink outline-none placeholder:text-mist/50"
      />

      {metaOpen ? (
        <div className="mb-5 grid gap-4 rounded-xl border border-line bg-white/70 p-4 sm:grid-cols-2">
          <label className="block space-y-2 text-sm">
            <span className="text-mist">Slug（可空，保存时按标题生成）</span>
            <input
              value={slug}
              onChange={(event) => {
                markDirty();
                setSlug(event.target.value);
              }}
              className="min-h-11 w-full rounded-xl border border-line bg-paper px-3 font-mono text-sm text-ink outline-none focus:border-seal"
            />
          </label>
          <label className="block space-y-2 text-sm">
            <span className="text-mist">分类</span>
            <select
              value={categoryId}
              onChange={(event) => {
                markDirty();
                setCategoryId(event.target.value);
              }}
              className="min-h-11 w-full rounded-xl border border-line bg-paper px-3 text-ink outline-none focus:border-seal"
            >
              <option value="">无分类</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <div className="space-y-2 text-sm sm:col-span-2">
            <span className="text-mist">标签</span>
            <div className="flex min-h-11 flex-wrap gap-2 rounded-xl border border-line bg-paper px-3 py-2">
              {tags.length === 0 ? (
                <span className="text-mist">暂无标签</span>
              ) : (
                tags.map((tag) => {
                  const active = tagIds.includes(tag.id);
                  return (
                    <button
                      key={tag.id}
                      type="button"
                      onClick={() => toggleTag(tag.id)}
                      className={`cursor-pointer rounded-full px-3 py-1 text-sm transition-colors ${
                        active
                          ? "bg-seal text-paper"
                          : "bg-white text-mist hover:text-ink"
                      }`}
                    >
                      {tag.name}
                    </button>
                  );
                })
              )}
            </div>
          </div>
        </div>
      ) : null}

      {editorReady ? (
        <MarkdownEditor
          initialValue={markdown}
          resetToken={resetToken}
          disabled={saveState === "conflict" || status === "TRASHED"}
          onChange={(value) => {
            markDirty();
            setMarkdown(value);
          }}
        />
      ) : null}
    </div>
  );
}
