"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { AdminButton } from "@/components/AdminButton";
import { SealMark } from "@/components/SealMark";
import { logout } from "@/lib/api/posts";

const navItems = [
  { href: "/admin/posts", label: "文章", match: (path: string) => path.startsWith("/admin/posts") },
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
    <div className="mb-8 flex flex-wrap items-end justify-between gap-4 border-b border-line pb-8">
      <div className="flex min-w-0 items-center gap-3">
        <SealMark size={34} className="shrink-0 text-seal seal-glow" />
        <div className="min-w-0">
          <p className="text-xs tracking-[0.35em] text-gold">{eyebrow}</p>
          <h1 className="font-serif text-3xl tracking-wide text-ink">{title}</h1>
          <nav className="mt-3 flex flex-wrap gap-1 text-sm" aria-label="管理导航">
            {navItems.map((item) => {
              const active = item.match(pathname);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={`admin-tab${active ? " is-active" : ""}`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>
      </div>
      <div className="flex flex-wrap gap-2">
        <AdminButton href="/">公开站</AdminButton>
        <AdminButton type="button" onClick={() => void onLogout()}>
          退出
        </AdminButton>
        {children}
      </div>
    </div>
  );
}
