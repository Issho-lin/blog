import Link from "next/link";
import { TaxonomyRow } from "@/components/TaxonomyMarks";
import type { PublicPostSummary } from "@/lib/api/types";

function formatDate(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}

function tagPairs(post: PublicPostSummary) {
  const slugs = post.tagSlugs ?? [];
  return (post.tagNames ?? []).map((name, index) => ({
    name,
    slug: slugs[index] ?? null,
  }));
}

export function PostList({
  posts,
  emptyText = "还没有已发布文章。去管理台写第一篇吧。",
}: {
  posts: PublicPostSummary[];
  emptyText?: string;
}) {
  if (posts.length === 0) {
    return (
      <div className="border-y border-line py-12 text-mist">{emptyText}</div>
    );
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {posts.map((post, index) => {
        const tags = tagPairs(post);
        return (
          <li
            key={post.id}
            className="soft-in py-7 sm:py-8"
            style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}
          >
            <article>
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-mist">
                <time>{formatDate(post.publishedAt)}</time>
                <span className="text-gold/70" aria-hidden>
                  ·
                </span>
                <span>{post.readingTimeMinutes} 分钟</span>
              </div>
              <h2 className="group relative mt-3 font-serif text-2xl leading-snug tracking-wide text-ink sm:text-[1.75rem]">
                <span
                  aria-hidden
                  className="absolute left-0 top-1/2 h-[0.85em] w-0.5 -translate-y-1/2 origin-center scale-y-0 bg-seal opacity-0 transition-[transform,opacity] duration-200 group-hover:scale-y-100 group-hover:opacity-100"
                />
                <Link
                  href={`/posts/${post.slug}`}
                  className="relative inline-block max-w-full cursor-pointer transition-transform duration-200 ease-out group-hover:translate-x-3"
                >
                  {post.title}
                  <span
                    aria-hidden
                    className="absolute inset-x-0 -bottom-1 h-px origin-left scale-x-0 bg-gradient-to-r from-gold to-transparent transition-transform duration-200 group-hover:scale-x-100"
                  />
                </Link>
              </h2>
              {post.summary ? (
                <p className="mt-3 max-w-2xl text-[1.02rem] leading-7 text-mist line-clamp-2">
                  {post.summary}
                </p>
              ) : null}
              <div className="mt-4">
                <TaxonomyRow
                  categoryName={post.categoryName}
                  categoryHref={
                    post.categorySlug ? `/categories/${post.categorySlug}` : null
                  }
                  tags={tags.map((tag) => ({
                    name: tag.name,
                    href: tag.slug ? `/tags/${tag.slug}` : null,
                  }))}
                />
              </div>
            </article>
          </li>
        );
      })}
    </ul>
  );
}
