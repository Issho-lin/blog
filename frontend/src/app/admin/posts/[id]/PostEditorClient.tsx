"use client";

import dynamic from "next/dynamic";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  useCallback,
  useEffect,
  useEffectEvent,
  useMemo,
  useRef,
  useState,
} from "react";
import { AdminButton } from "@/components/AdminButton";
import { AdminConfirmDialog } from "@/components/AdminConfirmDialog";
import { AdminInput, AdminTextarea, AdminTitleInput } from "@/components/AdminField";
import { AdminSelect } from "@/components/AdminSelect";
import { AdminUploadProgress } from "@/components/AdminUploadProgress";
import { SealMark } from "@/components/SealMark";
import { TaxonomyRow } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import {
  exportPost,
  getAdminPost,
  listCategories,
  listTags,
  permanentlyDeletePost,
  publishPost,
  restorePost,
  unpublishPost,
  updatePost,
  uploadImage,
  summarizeWithAi,
  suggestTaxonomyWithAi,
  writeWithAi,
} from "@/lib/api/posts";
import type { AdminPost, Category, Tag } from "@/lib/api/types";
import { readingStatsFromMarkdown } from "@/lib/reading-stats";
import {
  backupMatchesPost,
  clearEditorBackup,
  isLikelyOffline,
  readEditorBackup,
  writeEditorBackup,
  type EditorBackup,
} from "@/lib/editor-backup";

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

export function AdminPostEditor({ postId }: { postId: string }) {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [excerpt, setExcerpt] = useState("");
  const [coverUrl, setCoverUrl] = useState("");
  const [seoTitle, setSeoTitle] = useState("");
  const [seoDescription, setSeoDescription] = useState("");
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
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [pendingBackup, setPendingBackup] = useState<EditorBackup | null>(null);
  const [serverPeek, setServerPeek] = useState<AdminPost | null>(null);
  const [coverUploadPercent, setCoverUploadPercent] = useState<number | null>(null);
  const [coverUploadError, setCoverUploadError] = useState<string | null>(null);
  const [summarizing, setSummarizing] = useState(false);
  const [writeOpen, setWriteOpen] = useState(false);
  const [writeMode, setWriteMode] = useState("continue");
  const [writeInstruction, setWriteInstruction] = useState("");
  const [writePreview, setWritePreview] = useState("");
  const [writeBusy, setWriteBusy] = useState(false);
  const [taxonomyBusy, setTaxonomyBusy] = useState(false);
  const [tagPickerOpen, setTagPickerOpen] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const lastCoverFileRef = useRef<File | null>(null);
  const tagPickerRef = useRef<HTMLDivElement>(null);

  const coverFileRef = useRef<HTMLInputElement>(null);
  const versionRef = useRef(0);
  const dirtyRef = useRef(false);
  const savingRef = useRef(false);
  const latestPayloadRef = useRef({
    title: "",
    slug: "",
    markdown: "",
    excerpt: "",
    coverUrl: "",
    seoTitle: "",
    seoDescription: "",
    categoryId: "",
    tagIds: [] as string[],
  });

  const applyPost = useCallback((post: AdminPost, resetEditor = false) => {
    setTitle(post.title);
    setSlug(post.slug);
    setExcerpt(post.excerpt ?? "");
    setCoverUrl(post.coverUrl ?? "");
    setSeoTitle(post.seoTitle ?? "");
    setSeoDescription(post.seoDescription ?? "");
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

  function snapshotBackup() {
    const payload = latestPayloadRef.current;
    writeEditorBackup({
      postId,
      savedAt: Date.now(),
      baseVersion: versionRef.current,
      title: payload.title,
      slug: payload.slug,
      markdown: payload.markdown,
      excerpt: payload.excerpt,
      coverUrl: payload.coverUrl,
      seoTitle: payload.seoTitle,
      seoDescription: payload.seoDescription,
      categoryId: payload.categoryId,
      tagIds: payload.tagIds,
    });
  }

  function applyBackup(backup: EditorBackup, resetEditor: boolean) {
    setTitle(backup.title);
    setSlug(backup.slug);
    setExcerpt(backup.excerpt);
    setCoverUrl(backup.coverUrl);
    setSeoTitle(backup.seoTitle ?? "");
    setSeoDescription(backup.seoDescription ?? "");
    setMarkdown(backup.markdown);
    setCategoryId(backup.categoryId);
    setTagIds(backup.tagIds);
    dirtyRef.current = true;
    setSaveState("dirty");
    if (resetEditor) {
      setResetToken((token) => token + 1);
    }
  }

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

        const backup = readEditorBackup(postId);
        if (!backup || backupMatchesPost(backup, post)) {
          if (backup) clearEditorBackup(postId);
        } else if (backup.baseVersion === post.version) {
          applyBackup(backup, true);
          setSaveMessage("已恢复本机未同步备份");
        } else {
          setPendingBackup(backup);
        }
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
      excerpt,
      coverUrl,
      seoTitle,
      seoDescription,
      categoryId,
      tagIds,
    };
  }, [title, slug, markdown, excerpt, coverUrl, seoTitle, seoDescription, categoryId, tagIds]);

  const persist = useEffectEvent(async (manual = false, overwrite = false) => {
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
    snapshotBackup();

    try {
      let expectedVersion = versionRef.current;
      if (overwrite) {
        const current = await getAdminPost(postId);
        expectedVersion = current.version;
      }
      const updated = await updatePost(postId, {
        title: payload.title.trim(),
        markdownContent: payload.markdown,
        slug: payload.slug.trim(),
        categoryId: payload.categoryId || null,
        tagIds: payload.tagIds,
        expectedVersion,
        excerpt: payload.excerpt.trim(),
        coverUrl: payload.coverUrl.trim(),
        seoTitle: payload.seoTitle.trim(),
        seoDescription: payload.seoDescription.trim(),
      });
      versionRef.current = updated.version;
      setVersion(updated.version);
      setStatus(updated.status);
      setUpdatedAt(updated.updatedAt);
      setSlug(updated.slug);
      dirtyRef.current = false;
      clearEditorBackup(postId);
      setPendingBackup(null);
      setServerPeek(null);
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
      snapshotBackup();
      if (err instanceof ApiError && err.code === "CONCURRENT_MODIFICATION") {
        setSaveState("conflict");
        setSaveMessage("其他窗口已修改本文。可覆盖服务端、采用服务端，或先查看服务端版本。");
        return;
      }
      if (isLikelyOffline(err)) {
        setSaveState("error");
        setSaveMessage("网络中断，已备份到本机。恢复连接后会自动保存。");
        return;
      }
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "保存失败，已备份到本机。");
    } finally {
      savingRef.current = false;
    }
  });

  useEffect(() => {
    if (loading || saveState === "conflict") return;
    if (!dirtyRef.current) return;
    snapshotBackup();

    const timer = window.setTimeout(() => {
      void persist(false);
    }, 1000);

    return () => window.clearTimeout(timer);
  }, [title, slug, markdown, excerpt, coverUrl, seoTitle, seoDescription, categoryId, tagIds, loading, saveState, persist]);

  useEffect(() => {
    function onBeforeUnload(event: BeforeUnloadEvent) {
      if (!dirtyRef.current) return;
      snapshotBackup();
      event.preventDefault();
      event.returnValue = "";
    }
    function onOnline() {
      if (!dirtyRef.current) return;
      if (saveState === "conflict") return;
      void persist(true);
    }
    window.addEventListener("beforeunload", onBeforeUnload);
    window.addEventListener("online", onOnline);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      window.removeEventListener("online", onOnline);
    };
  }, [saveState, persist]);

  function addTag(id: string) {
    if (!id) return;
    markDirty();
    setTagIds((current) => (current.includes(id) ? current : [...current, id]));
  }

  function removeTag(id: string) {
    markDirty();
    setTagIds((current) => current.filter((item) => item !== id));
  }

  useEffect(() => {
    if (!tagPickerOpen) return;
    function onPointerDown(event: PointerEvent) {
      if (!tagPickerRef.current?.contains(event.target as Node)) {
        setTagPickerOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setTagPickerOpen(false);
    }
    window.addEventListener("pointerdown", onPointerDown);
    window.addEventListener("keydown", onKeyDown);
    return () => {
      window.removeEventListener("pointerdown", onPointerDown);
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [tagPickerOpen]);

  async function onSummarize() {
    setAiError(null);
    setSummarizing(true);
    try {
      const result = await summarizeWithAi(markdown);
      markDirty();
      setExcerpt(result.text.slice(0, 500));
      setMetaOpen(true);
    } catch (err) {
      setAiError(err instanceof ApiError ? err.message : "生成摘要失败");
    } finally {
      setSummarizing(false);
    }
  }

  async function onSuggestTaxonomy() {
    setAiError(null);
    setTaxonomyBusy(true);
    try {
      const result = await suggestTaxonomyWithAi(title, markdown);
      const [categoryList, tagList] = await Promise.all([listCategories(), listTags()]);
      setCategories(categoryList);
      setTags(tagList);
      markDirty();
      if (result.categoryId) {
        setCategoryId(result.categoryId);
      }
      setTagIds(result.tags.map((item) => item.id));
    } catch (err) {
      setAiError(err instanceof ApiError ? err.message : "AI 分类打标失败");
    } finally {
      setTaxonomyBusy(false);
    }
  }

  async function onWrite() {
    setAiError(null);
    setWriteBusy(true);
    try {
      const result = await writeWithAi({
        markdown: writeMode === "draft" ? "" : markdown,
        instruction:
          writeMode === "draft" && !writeInstruction.trim()
            ? title.trim()
            : writeInstruction,
        mode: writeMode,
      });
      setWritePreview(result.text);
    } catch (err) {
      setAiError(err instanceof ApiError ? err.message : "AI 帮写失败");
    } finally {
      setWriteBusy(false);
    }
  }

  function applyWritePreview() {
    if (!writePreview.trim()) return;
    markDirty();
    if (writeMode === "titles") {
      const firstLine = writePreview
        .split("\n")
        .map((line) => line.replace(/^\s*\d+[.)、]\s*/, "").trim())
        .find((line) => line.length > 0);
      if (firstLine) {
        setTitle(firstLine);
      }
    } else if (writeMode === "rewrite" || writeMode === "draft") {
      setMarkdown(writePreview);
      setResetToken((token) => token + 1);
    } else {
      const next = markdown.trim() ? `${markdown.trim()}\n\n${writePreview}` : writePreview;
      setMarkdown(next);
      setResetToken((token) => token + 1);
    }
    setWritePreview("");
    setWriteOpen(false);
  }

  async function onCoverFile(file: File | undefined) {
    if (!file) return;
    lastCoverFileRef.current = file;
    setCoverUploadError(null);
    setCoverUploadPercent(0);
    setActing(true);
    try {
      const uploaded = await uploadImage(file, {
        onProgress: (percent) => setCoverUploadPercent(percent),
      });
      markDirty();
      setCoverUrl(uploaded.url);
      setCoverUploadPercent(null);
    } catch (err) {
      setCoverUploadPercent(null);
      setCoverUploadError(err instanceof ApiError ? err.message : "封面上传失败");
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "封面上传失败");
    } finally {
      setActing(false);
    }
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
      setSaveMessage(status === "PUBLISHED" ? "已更新发布" : "已发布");
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
      clearEditorBackup(postId);
      setPendingBackup(null);
      setServerPeek(null);
    } catch (err) {
      setSaveMessage(err instanceof ApiError ? err.message : "重新加载失败");
    } finally {
      setActing(false);
    }
  }

  async function overwriteWithLocal() {
    setActing(true);
    try {
      await persist(true, true);
    } finally {
      setActing(false);
    }
  }

  async function peekServerVersion() {
    setActing(true);
    try {
      setServerPeek(await getAdminPost(postId));
    } catch (err) {
      setSaveMessage(err instanceof ApiError ? err.message : "读取服务端版本失败");
    } finally {
      setActing(false);
    }
  }

  async function onExport() {
    setActing(true);
    try {
      if (dirtyRef.current) {
        await persist(true);
        if (dirtyRef.current) return;
      }
      await exportPost(postId, `${slug || "post"}.md`);
      setSaveMessage("已导出 Markdown");
    } catch (err) {
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "导出失败");
    } finally {
      setActing(false);
    }
  }

  async function onPreview() {
    if (dirtyRef.current) {
      await persist(true);
      if (dirtyRef.current) return;
    }
    router.push(`/admin/posts/${postId}/preview`);
  }

  async function onBack() {
    if (dirtyRef.current) {
      await persist(true);
      if (dirtyRef.current) return;
    }
    router.push("/admin/posts");
  }

  async function onRestore() {
    setActing(true);
    try {
      const updated = await restorePost(postId);
      applyPost(updated, false);
      setSaveMessage("已从回收站恢复");
    } catch (err) {
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "恢复失败");
    } finally {
      setActing(false);
    }
  }

  function requestPermanentlyDelete() {
    setDeleteConfirmOpen(true);
  }

  function onDeleteConfirm() {
    setDeleteConfirmOpen(false);
    void onPermanentlyDelete();
  }

  async function onPermanentlyDelete() {
    setActing(true);
    try {
      await permanentlyDeletePost(postId);
      router.replace("/admin/posts");
    } catch (err) {
      setSaveState("error");
      setSaveMessage(err instanceof ApiError ? err.message : "彻底删除失败");
      setActing(false);
    }
  }

  const stats = useMemo(() => readingStatsFromMarkdown(markdown), [markdown]);
  const remainingTags = tags.filter((tag) => !tagIds.includes(tag.id));

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
              <span>{stats.words.toLocaleString("zh-CN")} 字</span>
              <span aria-hidden>·</span>
              <span>约 {stats.minutes} 分钟</span>
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
          <AdminButton type="button" onClick={() => setMetaOpen((open) => !open)}>
            {metaOpen ? "收起设置" : "文章设置"}
          </AdminButton>
          <AdminButton
            type="button"
            onClick={() => {
              setWriteOpen((open) => !open);
              setAiError(null);
            }}
          >
            {writeOpen ? "收起帮写" : "AI 帮我写"}
          </AdminButton>
          <AdminButton
            type="button"
            onClick={() => void onPreview()}
          >
            预览
          </AdminButton>
          <AdminButton href={`/admin/posts/${postId}/history`}>历史版本</AdminButton>
          <AdminButton
            type="button"
            disabled={acting}
            onClick={() => void onExport()}
          >
            导出
          </AdminButton>
          {status === "TRASHED" ? (
            <>
              <AdminButton
                type="button"
                variant="soft"
                disabled={acting}
                onClick={() => void onRestore()}
              >
                恢复
              </AdminButton>
              <AdminButton
                type="button"
                variant="danger"
                disabled={acting}
                onClick={() => requestPermanentlyDelete()}
              >
                彻底删除
              </AdminButton>
            </>
          ) : (
            <>
              {status === "PUBLISHED" ? (
                <AdminButton
                  type="button"
                  disabled={acting}
                  onClick={() => void onUnpublish()}
                >
                  下线
                </AdminButton>
              ) : null}
              <AdminButton
                type="button"
                variant="primary"
                disabled={acting}
                onClick={() => void onPublish()}
              >
                {status === "PUBLISHED" ? "更新发布" : "发布"}
              </AdminButton>
              <AdminButton
                type="button"
                disabled={acting || saveState === "saving"}
                onClick={() => void onBack()}
              >
                返回
              </AdminButton>
            </>
          )}
        </div>
      </div>

      {writeOpen ? (
        <div className="mb-4 space-y-3 rounded-xl border border-line bg-white/80 p-4">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <span className="text-sm text-mist">模式</span>
            <AdminSelect
              aria-label="帮写模式"
              value={writeMode}
              onValueChange={setWriteMode}
              className="min-w-[11.5rem]"
              options={[
                { value: "draft", label: "从零写" },
                { value: "continue", label: "续写" },
                { value: "rewrite", label: "润色" },
                { value: "outline", label: "按大纲写" },
                { value: "titles", label: "标题建议" },
              ]}
            />
          </div>
          <label className="block space-y-2 text-sm">
            <span className="mb-2 inline-block text-mist">指令（可选）</span>
            <AdminTextarea
              value={writeInstruction}
              onChange={(event) => setWriteInstruction(event.target.value)}
              rows={2}
              placeholder={
                writeMode === "draft"
                  ? "例如：写一篇介绍本仓库架构的文章（也可只用上方标题）"
                  : "例如：补充一节关于部署的说明"
              }
            />
          </label>
          <div className="flex flex-wrap gap-2">
            <AdminButton
              type="button"
              variant="primary"
              disabled={
                writeBusy ||
                (writeMode === "draft"
                  ? !writeInstruction.trim() && !title.trim()
                  : !markdown.trim() && writeMode !== "outline")
              }
              onClick={() => void onWrite()}
            >
              {writeBusy ? "生成中…" : "生成"}
            </AdminButton>
            {writePreview ? (
              <>
                <AdminButton type="button" variant="soft" onClick={applyWritePreview}>
                  {writeMode === "titles" ? "用作标题" : writeMode === "draft" ? "替换正文" : "插入正文"}
                </AdminButton>
                <AdminButton
                  type="button"
                  onClick={() => setWritePreview("")}
                >
                  丢弃
                </AdminButton>
              </>
            ) : null}
          </div>
          {writePreview ? (
            <pre className="max-h-64 overflow-auto whitespace-pre-wrap rounded-lg border border-line bg-paper p-3 text-sm">
              {writePreview}
            </pre>
          ) : null}
        </div>
      ) : null}

      {aiError ? (
        <p className="mb-4 text-sm text-seal">{aiError}</p>
      ) : null}

      {pendingBackup && saveState !== "conflict" ? (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-gold/40 bg-white/80 px-4 py-3 text-sm text-ink">
          <span>
            发现本机备份（
            {new Intl.DateTimeFormat("zh-CN", {
              month: "numeric",
              day: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            }).format(new Date(pendingBackup.savedAt))}
            ），与当前服务端版本不一致。
          </span>
          <div className="flex flex-wrap gap-2">
            <AdminButton
              type="button"
              variant="primary"
              onClick={() => {
                applyBackup(pendingBackup, true);
                setPendingBackup(null);
                setSaveMessage("已恢复本地备份，可继续保存");
              }}
            >
              保留本地
            </AdminButton>
            <AdminButton
              type="button"
              onClick={() => {
                clearEditorBackup(postId);
                setPendingBackup(null);
              }}
            >
              采用服务端
            </AdminButton>
          </div>
        </div>
      ) : null}

      {saveState === "conflict" ? (
        <div className="mb-4 rounded-xl border border-warn/30 bg-seal-soft/50 px-4 py-3 text-sm text-warn">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span>{saveMessage}</span>
            <div className="flex flex-wrap gap-2">
              <AdminButton
                type="button"
                variant="primary"
                disabled={acting}
                onClick={() => void overwriteWithLocal()}
              >
                用本地覆盖
              </AdminButton>
              <AdminButton
                type="button"
                disabled={acting}
                onClick={() => void reloadFromServer()}
              >
                采用服务端
              </AdminButton>
              <AdminButton
                type="button"
                disabled={acting}
                onClick={() => void peekServerVersion()}
              >
                查看服务端
              </AdminButton>
            </div>
          </div>
        </div>
      ) : null}

      {saveState === "error" ? (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-white/80 px-4 py-3 text-sm text-mist">
          <span>{saveMessage}</span>
          <AdminButton
            type="button"
            variant="primary"
            disabled={acting}
            onClick={() => void persist(true)}
          >
            重试保存
          </AdminButton>
        </div>
      ) : null}

      {serverPeek ? (
        <div className="mb-4 rounded-xl border border-line bg-white/80 px-4 py-3 text-sm">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="text-mist">服务端版本 v{serverPeek.version}</p>
              <p className="mt-1 font-medium text-ink">{serverPeek.title}</p>
            </div>
            <AdminButton type="button" onClick={() => setServerPeek(null)}>
              关闭
            </AdminButton>
          </div>
          <pre className="mt-3 max-h-60 overflow-auto whitespace-pre-wrap break-words rounded-lg bg-paper px-3 py-2 text-xs leading-6 text-ink">
            {serverPeek.markdownContent || "（无正文）"}
          </pre>
        </div>
      ) : null}

      <AdminTitleInput
        value={title}
        onChange={(event) => {
          markDirty();
          setTitle(event.target.value);
        }}
        placeholder="文章标题"
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <TaxonomyRow
          categoryName={categories.find((item) => item.id === categoryId)?.name}
          tags={tagIds
            .map((id) => tags.find((item) => item.id === id)?.name)
            .filter((name): name is string => Boolean(name))
            .map((name) => ({ name }))}
        />
        {!metaOpen ? (
          <button
            type="button"
            onClick={() => setMetaOpen(true)}
            className="cursor-pointer text-sm text-mist underline-offset-4 hover:text-seal hover:underline"
          >
            {categoryId || tagIds.length > 0 ? "改分类 / 标签" : "设置分类 / 标签"}
          </button>
        ) : null}
      </div>

      {metaOpen ? (
        <div className="mb-5 grid gap-4 rounded-xl border border-line bg-white/70 p-4 sm:grid-cols-2">
          <label className="block space-y-2 text-sm">
            <span className="mb-2 inline-block text-mist">Slug（可空，保存时按标题生成）</span>
            <AdminInput
              value={slug}
              onChange={(event) => {
                markDirty();
                setSlug(event.target.value);
              }}
              className="font-mono"
            />
          </label>
          <label className="block space-y-2 text-sm">
            <span className="mb-2 inline-block text-mist">分类 · 一卷一篇</span>
            <AdminSelect
              value={categoryId}
              onValueChange={(next) => {
                markDirty();
                setCategoryId(next);
              }}
              className="w-full"
              options={[
                { value: "", label: "无分类" },
                ...categories.map((category) => ({
                  value: category.id,
                  label: category.name,
                })),
              ]}
            />
          </label>
          <div className="space-y-2 text-sm sm:col-span-2">
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <span className="text-mist">摘要 · 用于首页卡片和 SEO，留空则截取正文</span>
              <AdminButton
                type="button"
                disabled={summarizing || !markdown.trim()}
                onClick={() => void onSummarize()}
              >
                {summarizing ? "生成中…" : "生成摘要"}
              </AdminButton>
            </div>
            <AdminTextarea
              value={excerpt}
              maxLength={500}
              onChange={(event) => {
                markDirty();
                setExcerpt(event.target.value);
              }}
              rows={3}
              placeholder="一句话介绍这篇文章"
            />
            <p className="text-xs text-mist">{excerpt.length}/500</p>
          </div>
          <div className="space-y-2 text-sm sm:col-span-2">
            <span className="mb-2 inline-block text-mist">封面 · 支持上传或填写图片 URL</span>
            {coverUrl ? (
              <div className="overflow-hidden rounded-xl border border-line bg-paper">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={coverUrl}
                  alt="封面预览"
                  className="h-40 w-full object-cover"
                />
              </div>
            ) : null}
            <div className="flex flex-wrap items-center gap-2">
              <AdminInput
                value={coverUrl}
                onChange={(event) => {
                  markDirty();
                  setCoverUrl(event.target.value);
                }}
                placeholder="/uploads/... 或 https://"
                className="min-w-0 flex-1 font-mono"
              />
              <input
                ref={coverFileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  event.target.value = "";
                  void onCoverFile(file);
                }}
              />
              <AdminButton type="button" onClick={() => coverFileRef.current?.click()}>
                上传
              </AdminButton>
              {coverUrl ? (
                <AdminButton
                  type="button"
                  onClick={() => {
                    markDirty();
                    setCoverUrl("");
                  }}
                >
                  清除
                </AdminButton>
              ) : null}
              {coverUploadError && lastCoverFileRef.current ? (
                <AdminButton
                  type="button"
                  onClick={() => void onCoverFile(lastCoverFileRef.current ?? undefined)}
                >
                  重试
                </AdminButton>
              ) : null}
            </div>
            {coverUploadPercent !== null ? (
              <AdminUploadProgress label="上传封面" percent={coverUploadPercent} />
            ) : null}
            {coverUploadError ? (
              <p className="text-xs text-seal">{coverUploadError}</p>
            ) : null}
          </div>
          <label className="block space-y-2 text-sm">
            <span className="mb-2 inline-block text-mist">SEO 标题 · 留空则用文章标题</span>
            <AdminInput
              value={seoTitle}
              maxLength={120}
              onChange={(event) => {
                markDirty();
                setSeoTitle(event.target.value);
              }}
              placeholder={title.trim() || "与标题相同"}
            />
            <p className="text-xs text-mist">{seoTitle.length}/120</p>
          </label>
          <div className="space-y-2 text-sm sm:col-span-2">
            <span className="mb-2 inline-block text-mist">SEO 描述 · 留空则用摘要</span>
            <AdminTextarea
              value={seoDescription}
              maxLength={500}
              onChange={(event) => {
                markDirty();
                setSeoDescription(event.target.value);
              }}
              rows={3}
              placeholder="搜索引擎和社交分享用的描述"
            />
            <p className="text-xs text-mist">{seoDescription.length}/500</p>
          </div>
          <div className="space-y-2 text-sm sm:col-span-2">
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <span className="text-mist">标签 · 可盖多枚</span>
              <AdminButton
                type="button"
                disabled={taxonomyBusy || (!markdown.trim() && !title.trim())}
                onClick={() => void onSuggestTaxonomy()}
              >
                {taxonomyBusy ? "识别中…" : "AI 分类打标"}
              </AdminButton>
            </div>
            <div className="flex min-h-11 flex-wrap items-center gap-2 rounded-xl border border-line bg-paper px-3 py-2">
              {tagIds.map((id) => {
                const tag = tags.find((item) => item.id === id);
                return (
                  <span
                    key={id}
                    className="inline-flex items-center gap-1 rounded-full border border-gold/50 bg-white py-1 pl-3 pr-1.5 text-sm text-ink"
                  >
                    #{tag?.name ?? id}
                    <button
                      type="button"
                      aria-label={`移除标签 ${tag?.name ?? id}`}
                      onClick={() => removeTag(id)}
                      className="flex h-5 w-5 cursor-pointer items-center justify-center rounded-full text-mist hover:bg-paper hover:text-seal"
                    >
                      ×
                    </button>
                  </span>
                );
              })}
              {tags.length === 0 ? (
                <span className="text-mist">暂无标签，请先到「分类标签」里新建</span>
              ) : remainingTags.length > 0 ? (
                <div className="relative" ref={tagPickerRef}>
                  <button
                    type="button"
                    aria-label="添加标签"
                    aria-expanded={tagPickerOpen}
                    onClick={() => setTagPickerOpen((open) => !open)}
                    className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-dashed border-line bg-white text-lg leading-none text-mist hover:border-gold/50 hover:text-ink"
                  >
                    +
                  </button>
                  {tagPickerOpen ? (
                    <div className="admin-select-content absolute left-0 top-[calc(100%+6px)] w-max min-w-0">
                      <div className="admin-select-viewport max-h-60 overflow-y-auto">
                        {remainingTags.map((tag) => (
                          <button
                            key={tag.id}
                            type="button"
                            className="admin-select-item w-full whitespace-nowrap text-left hover:bg-seal-soft hover:text-seal"
                            onClick={() => addTag(tag.id)}
                          >
                            #{tag.name}
                          </button>
                        ))}
                      </div>
                    </div>
                  ) : null}
                </div>
              ) : null}
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

      {deleteConfirmOpen ? (
        <AdminConfirmDialog
          title="彻底删除"
          message={`彻底删除「${title || "未命名"}」？此操作不可恢复。`}
          confirmLabel="确认彻底删除"
          onCancel={() => setDeleteConfirmOpen(false)}
          onConfirm={onDeleteConfirm}
        />
      ) : null}
    </div>
  );
}
