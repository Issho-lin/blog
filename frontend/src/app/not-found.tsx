import Link from "next/link";
import { SiteFooter, SiteHeader } from "@/components/SiteChrome";

export default function NotFound() {
  return (
    <div className="flex min-h-full flex-col">
      <SiteHeader />
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col justify-center px-5 py-20 sm:px-6">
        <p className="text-sm tracking-[0.35em] text-seal">404</p>
        <h1 className="mt-4 font-serif text-4xl tracking-wide text-ink sm:text-5xl">
          这页还没写
        </h1>
        <span className="gold-rule gold-rule-draw mt-6" aria-hidden />
        <p className="mt-5 max-w-md text-lg leading-8 text-mist">
          文章可能未发布、已下线，或链接有误。
        </p>
        <Link
          href="/"
          className="mt-10 inline-flex min-h-11 w-fit cursor-pointer items-center rounded-full bg-ink px-5 text-sm text-paper transition-colors duration-200 hover:bg-seal"
        >
          回到首页
        </Link>
      </main>
      <SiteFooter />
    </div>
  );
}
