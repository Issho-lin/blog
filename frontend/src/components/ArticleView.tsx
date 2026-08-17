import Link from "next/link";
import { PostContent } from "@/components/PostContent";
import { PrintArticleButton } from "@/components/PrintArticleButton";
import { TaxonomyRow } from "@/components/TaxonomyMarks";
import type { PublicPostDetail } from "@/lib/api/types";

function formatDate(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}

export function ArticleView({
  post,
  backHref,
  backLabel,
  showViewCount = true,
  showPrint = true,
}: {
  post: Pick<
    PublicPostDetail,
    | "title"
    | "html"
    | "summary"
    | "coverUrl"
    | "publishedAt"
    | "readingTimeMinutes"
    | "viewCount"
    | "categoryName"
    | "categorySlug"
    | "tagNames"
    | "tagSlugs"
    | "tableOfContents"
  >;
  backHref: string;
  backLabel: string;
  showViewCount?: boolean;
  showPrint?: boolean;
}) {
  const hasToc = (post.tableOfContents?.length ?? 0) > 0;
  const dateLabel = formatDate(post.publishedAt) || "尚未发布";

  return (
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
            href={backHref}
            className="cursor-pointer text-sm text-mist transition-colors duration-200 hover:text-seal"
          >
            {backLabel}
          </Link>
          {showPrint ? <PrintArticleButton /> : null}
        </div>

        <header className="mt-8 space-y-5 border-b border-line pb-10">
          <div className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-mist">
            <time>{dateLabel}</time>
            <span className="text-gold/70" aria-hidden>
              ·
            </span>
            <span>{post.readingTimeMinutes} 分钟阅读</span>
            {showViewCount ? (
              <>
                <span className="print-hide text-gold/70" aria-hidden>
                  ·
                </span>
                <span className="print-hide">{post.viewCount} 次浏览</span>
              </>
            ) : null}
          </div>
          <h1 className="font-serif text-[clamp(1.9rem,4.5vw,2.75rem)] leading-snug tracking-wide text-ink">
            {post.title}
          </h1>
          {post.coverUrl ? (
            <div className="overflow-hidden rounded-xl border border-line">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={post.coverUrl}
                alt=""
                className="max-h-[28rem] w-full object-cover"
              />
            </div>
          ) : null}
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

      {hasToc ? (
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
  );
}
