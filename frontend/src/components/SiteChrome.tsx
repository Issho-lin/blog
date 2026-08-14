"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { SealMark } from "@/components/SealMark";

const navItems = [
  {
    href: "/",
    label: "文章",
    match: (path: string) => path === "/" || path.startsWith("/posts/"),
  },
  {
    href: "/archive",
    label: "归档",
    match: (path: string) => path === "/archive",
  },
  {
    href: "/categories",
    label: "分类",
    match: (path: string) => path.startsWith("/categories"),
  },
  {
    href: "/tags",
    label: "标签",
    match: (path: string) => path.startsWith("/tags"),
  },
  {
    href: "/search",
    label: "搜索",
    match: (path: string) => path.startsWith("/search"),
  },
  {
    href: "/about",
    label: "关于",
    match: (path: string) => path === "/about",
  },
  {
    href: "/admin/posts",
    label: "管理",
    match: (path: string) => path.startsWith("/admin"),
  },
] as const;

export function SiteHeader({ siteName }: { siteName: string }) {
  const pathname = usePathname();

  return (
    <header className="relative z-20">
      <div className="mx-auto flex w-full max-w-3xl items-center justify-between px-5 py-5 sm:px-6 sm:py-6">
        <Link href="/" className="group flex cursor-pointer items-center gap-2.5">
          <SealMark
            size={28}
            className="text-seal seal-glow transition-transform duration-300 group-hover:rotate-[-8deg] group-hover:scale-105"
          />
          <span className="font-serif text-lg tracking-[0.08em] text-ink transition-colors duration-200 group-hover:text-seal sm:text-xl">
            {siteName}
          </span>
        </Link>
        <nav className="flex flex-wrap items-center justify-end gap-x-0.5 gap-y-1 text-sm" aria-label="站点导航">
          {navItems.map((item) => {
            const active = item.match(pathname);
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`nav-link${active ? " is-active" : ""}`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      </div>
      <div className="mx-auto h-px w-full max-w-3xl bg-gradient-to-r from-transparent via-gold/40 to-transparent" />
    </header>
  );
}

export function SiteFooter({ siteName }: { siteName: string }) {
  return (
    <footer className="mt-auto">
      <div className="mx-auto flex w-full max-w-3xl items-center justify-between gap-4 px-5 py-8 text-sm text-mist sm:px-6">
        <span>© {new Date().getFullYear()} {siteName}</span>
        <span className="inline-flex items-center gap-2">
          <SealMark size={16} className="text-seal/70" />
          <span className="tracking-[0.18em] text-gold">落印開卷</span>
        </span>
      </div>
    </footer>
  );
}
