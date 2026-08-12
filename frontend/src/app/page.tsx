import { SiteFooter, SiteHeader } from "@/components/SiteChrome";
import { PostList } from "@/components/PostList";
import { SealMark } from "@/components/SealMark";
import { HomeEntrance } from "@/components/HomeEntrance";
import { listPublishedPosts } from "@/lib/api/posts";
import { ApiError } from "@/lib/api/client";
import type { PublicPostSummary } from "@/lib/api/types";

const siteName = process.env.NEXT_PUBLIC_SITE_NAME ?? "Linqibin Blog";

export default async function HomePage() {
  let posts: PublicPostSummary[] = [];
  let loadError: string | null = null;

  try {
    const page = await listPublishedPosts(1, 20);
    posts = page.items;
  } catch (error) {
    loadError =
      error instanceof ApiError
        ? error.message
        : "暂时无法连接后端，请确认后端已启动。";
  }

  return (
    <HomeEntrance siteName={siteName}>
      <div className="flex min-h-full flex-col">
        <SiteHeader />
        <main className="mx-auto w-full max-w-3xl flex-1 px-5 pb-20 sm:px-6">
          <section className="soft-in relative overflow-hidden border-b border-line pb-14 pt-10 sm:pb-16 sm:pt-14">
            <SealMark
              size={140}
              className="pointer-events-none absolute -right-3 top-2 rotate-12 text-seal/[0.09] sm:right-0 sm:top-6 sm:h-44 sm:w-44"
            />
            <p className="mb-5 text-xs tracking-[0.45em] text-seal">书斋 · 技术手稿</p>
            <div className="relative">
              <h1 className="max-w-[11em] font-serif text-[clamp(2.5rem,7.5vw,4rem)] leading-[1.12] tracking-[0.06em] text-ink">
                {siteName}
              </h1>
              <span className="gold-rule gold-rule-draw mt-6" aria-hidden />
              <p className="mt-6 max-w-md text-lg leading-8 text-mist">
                记录技术学习与工程实践。写给自己，也留给路过的人。
              </p>
            </div>
          </section>

          <section className="soft-in-delay pt-12">
            <div className="mb-6 flex items-baseline justify-between gap-4">
              <h2 className="font-serif text-xl tracking-[0.12em] text-ink">文章</h2>
              <span className="text-sm tracking-wider text-mist">{posts.length} 篇</span>
            </div>
            {loadError ? (
              <div className="border border-warn/25 bg-seal-soft/60 px-4 py-5 text-sm text-warn">
                {loadError}
              </div>
            ) : (
              <PostList posts={posts} />
            )}
          </section>
        </main>
        <SiteFooter />
      </div>
    </HomeEntrance>
  );
}
