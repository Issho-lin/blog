"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { AdminButton } from "@/components/AdminButton";
import { AdminInput } from "@/components/AdminField";
import { SealMark } from "@/components/SealMark";
import { ApiError } from "@/lib/api/client";
import { forgotPassword } from "@/lib/api/posts";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const result = await forgotPassword(email);
      setMessage(result.message);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-full items-center justify-center px-5 py-16">
      <div className="soft-in w-full max-w-md rounded-2xl border border-line bg-white/75 p-8 shadow-[0_20px_60px_rgba(20,24,31,0.06)] sm:p-10">
        <div className="flex items-center gap-3">
          <SealMark size={36} className="text-seal seal-glow" />
          <div>
            <p className="text-xs tracking-[0.35em] text-gold">管理台</p>
            <h1 className="font-serif text-3xl tracking-wide text-ink">找回密码</h1>
          </div>
        </div>
        <span className="gold-rule gold-rule-draw mt-5" aria-hidden />
        <p className="mt-4 text-sm leading-7 text-mist">
          提交后若邮箱存在，会发送重置说明。本地开发未配邮件时，链接会写在后端日志里。
        </p>
        <form onSubmit={(event) => void onSubmit(event)} className="mt-8 space-y-5">
          <label className="block space-y-2 text-sm">
            <span className="text-mist">邮箱</span>
            <AdminInput
              type="email"
              required
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          {error ? (
            <p className="rounded-xl bg-seal-soft px-3 py-2 text-sm text-warn">{error}</p>
          ) : null}
          {message ? (
            <p className="rounded-xl border border-line bg-white/70 px-3 py-2 text-sm text-mist">
              {message}
            </p>
          ) : null}
          <AdminButton type="submit" variant="primary" disabled={submitting} className="w-full">
            {submitting ? "提交中…" : "发送重置说明"}
          </AdminButton>
        </form>
        <p className="mt-8 text-center text-sm text-mist">
          <Link href="/admin/login" className="hover:text-seal">
            返回登录
          </Link>
        </p>
      </div>
    </div>
  );
}
