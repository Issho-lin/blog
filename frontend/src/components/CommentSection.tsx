"use client";

import { FormEvent, useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import { createPublicComment, listPublicComments } from "@/lib/api/posts";
import type { PublicComment } from "@/lib/api/types";

function formatTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function CommentSection({ slug }: { slug: string }) {
  const [comments, setComments] = useState<PublicComment[]>([]);
  const [authorName, setAuthorName] = useState("");
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    listPublicComments(slug)
      .then((items) => {
        if (!cancelled) setComments(items);
      })
      .catch(() => {
        if (!cancelled) setComments([]);
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const created = await createPublicComment(slug, authorName, content);
      setComments((current) => [...current, created]);
      setContent("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "发送失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="print-hide mt-16 border-t border-line pt-10" aria-labelledby="comments-heading">
      <div className="flex items-end justify-between gap-4">
        <div>
          <p className="text-xs tracking-[0.28em] text-gold">纸尾留白</p>
          <h2 id="comments-heading" className="mt-2 font-serif text-2xl tracking-wide text-ink">
            评论
          </h2>
        </div>
        <p className="text-sm text-mist">{comments.length} 条</p>
      </div>

      <ol className="mt-8 space-y-6">
        {comments.length === 0 ? (
          <li className="text-sm leading-7 text-mist">还没有评论。读完有想法，可以写在下面。</li>
        ) : (
          comments.map((comment) => (
            <li key={comment.id} className="border-l-2 border-gold/40 pl-4">
              <p className="text-sm text-ink">
                <span className="font-medium">{comment.authorName}</span>
                <span className="mx-2 text-gold/70">·</span>
                <time className="text-mist">{formatTime(comment.createdAt)}</time>
              </p>
              <p className="mt-2 whitespace-pre-wrap text-[15px] leading-7 text-ink">
                {comment.content}
              </p>
            </li>
          ))
        )}
      </ol>

      <form className="mt-10 grid gap-4" onSubmit={(event) => void onSubmit(event)}>
        <label className="grid gap-1 text-sm">
          <span className="text-mist">称呼</span>
          <input
            className="admin-field"
            value={authorName}
            onChange={(event) => setAuthorName(event.target.value)}
            maxLength={40}
            required
          />
        </label>
        <label className="grid gap-1 text-sm">
          <span className="text-mist">留言</span>
          <textarea
            className="admin-field admin-field-area min-h-28"
            value={content}
            onChange={(event) => setContent(event.target.value)}
            maxLength={2000}
            required
          />
        </label>
        {error ? (
          <p className="rounded-xl bg-seal-soft px-3 py-2 text-sm text-warn">{error}</p>
        ) : null}
        <div>
          <button
            type="submit"
            disabled={submitting}
            className="admin-btn admin-btn-primary"
          >
            {submitting ? "发送中…" : "写下这条"}
          </button>
        </div>
      </form>
    </section>
  );
}
