"use client";

import { FormEvent, Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AdminButton } from "@/components/AdminButton";
import { AdminInput } from "@/components/AdminField";
import { SealMark } from "@/components/SealMark";
import { ApiError } from "@/lib/api/client";
import { resetPassword } from "@/lib/api/posts";

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (newPassword !== confirmPassword) {
      setError("两次输入的密码不一致");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await resetPassword(token, newPassword);
      router.replace("/admin/login");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "重置失败");
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
            <h1 className="font-serif text-3xl tracking-wide text-ink">设置新密码</h1>
          </div>
        </div>
        <span className="gold-rule gold-rule-draw mt-5" aria-hidden />
        {!token ? (
          <p className="mt-6 text-sm text-warn">缺少重置令牌，请重新申请找回密码。</p>
        ) : (
          <form onSubmit={(event) => void onSubmit(event)} className="mt-8 space-y-5">
            <label className="block space-y-2 text-sm">
              <span className="text-mist">新密码</span>
              <AdminInput
                type="password"
                required
                minLength={8}
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
              />
            </label>
            <label className="block space-y-2 text-sm">
              <span className="text-mist">确认新密码</span>
              <AdminInput
                type="password"
                required
                minLength={8}
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </label>
            {error ? (
              <p className="rounded-xl bg-seal-soft px-3 py-2 text-sm text-warn">{error}</p>
            ) : null}
            <AdminButton type="submit" variant="primary" disabled={submitting} className="w-full">
              {submitting ? "保存中…" : "保存新密码"}
            </AdminButton>
          </form>
        )}
        <p className="mt-8 text-center text-sm text-mist">
          <Link href="/admin/login" className="hover:text-seal">
            返回登录
          </Link>
        </p>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordForm />
    </Suspense>
  );
}
