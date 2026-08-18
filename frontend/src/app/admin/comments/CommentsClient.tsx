"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { AdminChrome } from "@/components/AdminChrome";
import { AdminConfirmDialog } from "@/components/AdminConfirmDialog";
import { ApiError } from "@/lib/api/client";
import { deleteAdminComment, listAdminComments } from "@/lib/api/posts";
import type { AdminComment } from "@/lib/api/types";

function formatTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function AdminCommentsView() {
  const router = useRouter();
  const [comments, setComments] = useState<AdminComment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pendingId, setPendingId] = useState<string | null>(null);

  function load() {
    listAdminComments()
      .then(setComments)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/admin/login");
          return;
        }
        setError(err instanceof ApiError ? err.message : "加载失败");
      });
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router]);

  async function onDelete(id: string) {
    try {
      await deleteAdminComment(id);
      setComments((current) => current.filter((item) => item.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "删除失败");
    } finally {
      setPendingId(null);
    }
  }

  return (
    <div className="mx-auto min-h-full w-full max-w-4xl px-5 py-10 sm:px-6">
      <AdminChrome title="评论" />
      {error ? (
        <p className="mb-6 rounded-xl bg-seal-soft px-4 py-3 text-sm text-warn">{error}</p>
      ) : null}
      {comments.length === 0 ? (
        <p className="text-sm text-mist">还没有评论。</p>
      ) : (
        <ul className="space-y-4">
          {comments.map((comment) => (
            <li
              key={comment.id}
              className="rounded-2xl border border-line bg-white/70 px-5 py-4"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm text-mist">
                    {comment.authorName}
                    <span className="mx-2 text-gold/70">·</span>
                    {formatTime(comment.createdAt)}
                  </p>
                  {comment.postSlug ? (
                    <Link
                      href={`/posts/${comment.postSlug}`}
                      className="mt-1 block font-serif text-lg tracking-wide text-ink hover:text-seal"
                    >
                      {comment.postTitle}
                    </Link>
                  ) : (
                    <p className="mt-1 font-serif text-lg text-mist">{comment.postTitle}</p>
                  )}
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-ink">
                    {comment.content}
                  </p>
                </div>
                <AdminButton
                  type="button"
                  variant="danger"
                  onClick={() => setPendingId(comment.id)}
                >
                  删除
                </AdminButton>
              </div>
            </li>
          ))}
        </ul>
      )}
      {pendingId ? (
        <AdminConfirmDialog
          title="删除这条评论？"
          message="删除后无法恢复。"
          confirmLabel="删除"
          onCancel={() => setPendingId(null)}
          onConfirm={() => void onDelete(pendingId)}
        />
      ) : null}
    </div>
  );
}
