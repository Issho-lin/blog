"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { AdminButton } from "@/components/AdminButton";
import { SealMark } from "@/components/SealMark";
import { ApiError } from "@/lib/api/client";
import { login } from "@/lib/api/posts";

export default function AdminLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@blog.com");
  const [password, setPassword] = useState("admin123");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(email, password);
      router.replace("/admin/posts");
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "登录失败，请稍后重试");
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
            <h1 className="font-serif text-3xl tracking-wide text-ink">登录</h1>
          </div>
        </div>
        <span className="gold-rule gold-rule-draw mt-5" aria-hidden />
        <p className="mt-4 text-sm leading-7 text-mist">使用作者账号进入文章管理。</p>

        <form onSubmit={onSubmit} className="mt-8 space-y-5">
          <label className="block space-y-2 text-sm">
            <span className="text-mist">邮箱</span>
            <input
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="min-h-11 w-full rounded-xl border border-line bg-paper px-3 py-2.5 text-base text-ink outline-none transition-colors duration-200 focus:border-seal"
            />
          </label>
          <label className="block space-y-2 text-sm">
            <span className="text-mist">密码</span>
            <input
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="min-h-11 w-full rounded-xl border border-line bg-paper px-3 py-2.5 text-base text-ink outline-none transition-colors duration-200 focus:border-seal"
            />
          </label>

          {error ? (
            <p className="rounded-xl bg-seal-soft px-3 py-2 text-sm text-warn">{error}</p>
          ) : null}

          <AdminButton
            type="submit"
            variant="primary"
            disabled={submitting}
            className="w-full"
          >
            {submitting ? "登录中…" : "登录"}
          </AdminButton>
        </form>

        <p className="mt-8 text-center text-sm text-mist">
          <Link href="/" className="cursor-pointer transition-colors duration-200 hover:text-seal">
            返回公开站点
          </Link>
        </p>
      </div>
    </div>
  );
}
