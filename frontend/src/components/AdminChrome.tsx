"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { AdminButton } from "@/components/AdminButton";
import { SealMark } from "@/components/SealMark";
import { logout } from "@/lib/api/posts";

const navItems = [
  { href: "/admin", label: "控制台", match: (path: string) => path === "/admin" },
  { href: "/admin/posts", label: "文章", match: (path: string) => path.startsWith("/admin/posts") },
  { href: "/admin/comments", label: "评论", match: (path: string) => path.startsWith("/admin/comments") },
  { href: "/admin/taxonomy", label: "分类标签", match: (path: string) => path.startsWith("/admin/taxonomy") },
  { href: "/admin/settings", label: "站点设置", match: (path: string) => path.startsWith("/admin/settings") },
] as const;

export function AdminChrome({
  title,
  eyebrow = "管理台",
  children,
}: {
  title: string;
  eyebrow?: string;
  children?: ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();

  async function onLogout() {
    await logout().catch(() => undefined);
    router.replace("/admin/login");
  }

  return (
    <header className="mb-8">
      <div className="flex items-center justify-between gap-4 py-4">
        <div className="flex min-w-0 items-center gap-3">
          <SealMark size={28} className="shrink-0 text-seal seal-glow" />
          <p className="text-xs tracking-[0.35em] text-gold">{eyebrow}</p>
        </div>
        <div className="flex shrink-0 gap-2">
          <AdminButton href="/">公开站</AdminButton>
          <AdminButton type="button" onClick={() => void onLogout()}>
            退出
          </AdminButton>
        </div>
      </div>

      <nav
        className="flex gap-1 overflow-x-auto border-b border-line text-sm"
        aria-label="管理导航"
      >
        {navItems.map((item) => {
          const active = item.match(pathname);
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? "page" : undefined}
              className={`admin-tab shrink-0${active ? " is-active" : ""}`}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="flex flex-col gap-4 pt-8 pb-6 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-serif text-3xl tracking-wide text-ink">{title}</h1>
        {children ? (
          <div className="flex shrink-0 flex-nowrap gap-2">{children}</div>
        ) : null}
      </div>
    </header>
  );
}
