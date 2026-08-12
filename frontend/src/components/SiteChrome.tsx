import Link from "next/link";
import { SealMark } from "@/components/SealMark";

const siteName = process.env.NEXT_PUBLIC_SITE_NAME ?? "Linqibin Blog";

export function SiteHeader() {
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
        <nav className="flex items-center gap-1 text-sm text-mist">
          <Link
            href="/"
            className="cursor-pointer rounded-md px-3 py-2 transition-colors duration-200 hover:text-ink"
          >
            文章
          </Link>
          <Link
            href="/admin/login"
            className="cursor-pointer rounded-md px-3 py-2 transition-colors duration-200 hover:text-ink"
          >
            管理
          </Link>
        </nav>
      </div>
      <div className="mx-auto h-px w-full max-w-3xl bg-gradient-to-r from-transparent via-gold/40 to-transparent" />
    </header>
  );
}

export function SiteFooter() {
  return (
    <footer className="mt-auto">
      <div className="mx-auto flex w-full max-w-3xl items-center justify-between gap-4 px-5 py-8 text-sm text-mist sm:px-6">
        <span>© {new Date().getFullYear()} {siteName}</span>
        <span className="inline-flex items-center gap-2">
          <SealMark size={16} className="text-seal/70" />
          <span className="tracking-[0.18em] text-gold">落印开卷</span>
        </span>
      </div>
    </footer>
  );
}
