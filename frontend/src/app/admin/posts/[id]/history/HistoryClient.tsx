"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { AdminChrome } from "@/components/AdminChrome";
import { AdminConfirmDialog } from "@/components/AdminConfirmDialog";
import { AdminSelect } from "@/components/AdminSelect";
import { ApiError } from "@/lib/api/client";
import { diffLines } from "@/lib/diff-lines";
import {
  getAdminPost,
  getPostRevision,
  listPostRevisions,
  restorePostRevision,
} from "@/lib/api/posts";
import type { PostRevisionDetail, PostRevisionSummary } from "@/lib/api/types";

const kindLabel = {
  AUTO: "自动保存",
  PUBLISH: "发布",
  RESTORE: "恢复",
} as const;

function formatTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function PostHistoryView({ postId }: { postId: string }) {
  const router = useRouter();
  const [title, setTitle] = useState("历史版本");
  const [revisions, setRevisions] = useState<PostRevisionSummary[]>([]);
  const [leftId, setLeftId] = useState("");
  const [rightId, setRightId] = useState("");
  const [left, setLeft] = useState<PostRevisionDetail | null>(null);
  const [right, setRight] = useState<PostRevisionDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [restoreId, setRestoreId] = useState<string | null>(null);
  const [acting, setActing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getAdminPost(postId), listPostRevisions(postId)])
      .then(([post, items]) => {
        if (cancelled) return;
        setTitle(post.title);
        setRevisions(items);
        if (items.length >= 1) {
          setRightId(items[0].id);
          setLeftId(items[Math.min(1, items.length - 1)].id);
        }
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setError(err instanceof ApiError ? err.message : "加载失败");
      });
    return () => {
      cancelled = true;
    };
  }, [postId, router]);

  useEffect(() => {
    if (!leftId) {
      setLeft(null);
      return;
    }
    getPostRevision(postId, leftId).then(setLeft).catch(() => setLeft(null));
  }, [leftId, postId]);

  useEffect(() => {
    if (!rightId) {
      setRight(null);
      return;
    }
    getPostRevision(postId, rightId).then(setRight).catch(() => setRight(null));
  }, [rightId, postId]);

  const diff = useMemo(() => {
    if (!left || !right) return [];
    return diffLines(left.markdownContent, right.markdownContent);
  }, [left, right]);

  const options = revisions.map((revision) => ({
    value: revision.id,
    label: `${formatTime(revision.createdAt)} · ${kindLabel[revision.kind]} · ${revision.title}`,
  }));

  async function onRestore() {
    if (!restoreId) return;
    setActing(true);
    try {
      await restorePostRevision(postId, restoreId);
      router.push(`/admin/posts/${postId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "恢复失败");
      setActing(false);
      setRestoreId(null);
    }
  }

  return (
    <div className="mx-auto min-h-full w-full max-w-5xl px-5 py-10 sm:px-6">
      <AdminChrome title="历史版本">
        <AdminButton href={`/admin/posts/${postId}`}>返回编辑</AdminButton>
      </AdminChrome>

      <p className="mb-6 text-sm text-mist">
        对比「{title}」的保存点。恢复会写回编辑中的标题和正文，不会自动发布。
      </p>

      {error ? (
        <p className="mb-6 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}

      {revisions.length === 0 ? (
        <p className="text-sm text-mist">还没有版本记录。保存或发布后会出现在这里。</p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="grid gap-1 text-sm">
              <span className="text-mist">左侧（旧）</span>
              <AdminSelect
                value={leftId}
                onValueChange={setLeftId}
                options={options}
              />
            </label>
            <label className="grid gap-1 text-sm">
              <span className="text-mist">右侧（新）</span>
              <AdminSelect
                value={rightId}
                onValueChange={setRightId}
                options={options}
              />
            </label>
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <AdminButton
              type="button"
              disabled={!leftId || acting}
              onClick={() => setRestoreId(leftId)}
            >
              恢复左侧
            </AdminButton>
            <AdminButton
              type="button"
              disabled={!rightId || acting}
              onClick={() => setRestoreId(rightId)}
            >
              恢复右侧
            </AdminButton>
          </div>

          <pre className="mt-8 overflow-auto rounded-2xl border border-line bg-white/80 p-4 text-[13px] leading-6">
            {diff.map((line, index) => (
              <div
                key={`${line.type}-${index}`}
                className={
                    line.type === "add"
                    ? "bg-[rgba(184,146,63,0.14)] text-ink"
                    : line.type === "remove"
                      ? "bg-seal-soft text-warn"
                      : "text-ink"
                }
              >
                <span className="mr-3 inline-block w-4 text-mist">
                  {line.type === "add" ? "+" : line.type === "remove" ? "-" : " "}
                </span>
                {line.text || " "}
              </div>
            ))}
          </pre>

          <p className="mt-6 text-sm text-mist">
            标题：{left?.title ?? "—"} → {right?.title ?? "—"}
          </p>
        </>
      )}

      {restoreId ? (
        <AdminConfirmDialog
          title="恢复这个版本？"
          message="当前编辑中的标题和正文会被替换。已发布文章需再点「更新发布」才会出现在公开页。"
          confirmLabel="恢复"
          confirmVariant="primary"
          onCancel={() => setRestoreId(null)}
          onConfirm={() => void onRestore()}
        />
      ) : null}

      <p className="mt-10 text-sm">
        <Link href={`/admin/posts/${postId}`} className="text-mist hover:text-seal">
          返回编辑器
        </Link>
      </p>
    </div>
  );
}
