import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { SiteFooter, SiteHeader } from "@/components/SiteChrome";
import { PostContent } from "@/components/PostContent";
import { PrintArticleButton } from "@/components/PrintArticleButton";
import { TaxonomyRow } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import { getPublishedPost } from "@/lib/api/posts";
import { loadPublicSiteSettings } from "@/lib/site-settings";

type PageProps = {
  params: Promise<{ slug: string }>;
};

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  try {
    const post = await getPublishedPost(slug);
    return {
      title: post.seoTitle ?? post.title,
      description: post.seoDescription ?? post.summary ?? undefined,
      openGraph: {
        title: post.seoTitle ?? post.title,
        description: post.seoDescription ?? post.summary ?? undefined,
        type: "article",
      },
    };
  } catch {
    return { title: "文章未找到" };
  }
}

function formatDate(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}

export default async function PostDetailPage({ params }: PageProps) {
  const { slug } = await params;
  const settings = await loadPublicSiteSettings();

  try {
    const post = await getPublishedPost(slug);
    const hasToc = (post.tableOfContents?.length ?? 0) > 0;

    return (
      <div className="flex min-h-full flex-col">
        <SiteHeader siteName={settings.siteName} />
        <main
          className={
            hasToc
              ? "mx-auto grid w-full max-w-5xl flex-1 gap-12 px-5 py-8 sm:px-6 lg:grid-cols-[minmax(0,42rem)_1fr] lg:justify-center"
              : "mx-auto w-full max-w-3xl flex-1 px-5 py-8 sm:px-6"
          }
        >
          <article id="article-print" className="soft-in min-w-0">
            <div className="print-hide flex flex-wrap items-center justify-between gap-3">
              <Link
                href="/"
                className="cursor-pointer text-sm text-mist transition-colors duration-200 hover:text-seal"
              >
                ← 返回文章列表
              </Link>
              <PrintArticleButton />
            </div>

            <header className="mt-8 space-y-5 border-b border-line pb-10">
              <div className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-mist">
                <time>{formatDate(post.publishedAt)}</time>
                <span className="text-gold/70" aria-hidden>
                  ·
                </span>
                <span>{post.readingTimeMinutes} 分钟阅读</span>
                <span className="print-hide text-gold/70" aria-hidden>
                  ·
                </span>
                <span className="print-hide">{post.viewCount} 次浏览</span>
              </div>
              <h1 className="font-serif text-[clamp(1.9rem,4.5vw,2.75rem)] leading-snug tracking-wide text-ink">
                {post.title}
              </h1>
              <span className="gold-rule gold-rule-draw" aria-hidden />
              {post.summary ? (
                <p className="text-lg leading-8 text-mist line-clamp-3">{post.summary}</p>
              ) : null}
              <TaxonomyRow
                categoryName={post.categoryName}
                categoryHref={
                  post.categorySlug ? `/categories/${post.categorySlug}` : null
                }
                tags={post.tagNames.map((name, index) => ({
                  name,
                  href: post.tagSlugs?.[index]
                    ? `/tags/${post.tagSlugs[index]}`
                    : null,
                }))}
              />
            </header>

            <PostContent html={post.html} />
          </article>

          {post.tableOfContents?.length > 0 ? (
            <aside className="soft-in-delay hidden lg:block">
              <div className="sticky top-10">
                <p className="mb-4 text-sm tracking-[0.2em] text-gold">目录</p>
                <ul className="space-y-3 border-l border-gold/35 text-sm leading-6 text-mist">
                  {post.tableOfContents.map((item) => (
                    <li
                      key={item.anchor}
                      style={{ paddingLeft: `${12 + Math.max(item.level - 1, 0) * 12}px` }}
                    >
                      <a
                        href={`#${item.anchor}`}
                        className="cursor-pointer transition-colors duration-200 hover:text-seal"
                      >
                        {item.text}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            </aside>
          ) : null}
        </main>
        <SiteFooter siteName={settings.siteName} />
      </div>
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
