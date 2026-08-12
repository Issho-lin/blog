import Link from "next/link";
import type { PublicPostSummary } from "@/lib/api/types";

function formatDate(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));
}

export function PostList({ posts }: { posts: PublicPostSummary[] }) {
  if (posts.length === 0) {
    return (
      <div className="border-y border-line py-12 text-mist">
        还没有已发布文章。去管理台写第一篇吧。
      </div>
    );
  }

  return (
    <ul className="divide-y divide-line border-y border-line">
      {posts.map((post, index) => (
        <li
          key={post.id}
          className="soft-in"
          style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}
        >
          <Link
            href={`/posts/${post.slug}`}
            className="group relative block cursor-pointer py-7 transition-colors duration-200 hover:bg-white/35 sm:py-8"
          >
            <span
              aria-hidden
              className="absolute left-0 top-1/2 h-8 w-0.5 -translate-y-1/2 scale-y-0 bg-seal opacity-0 transition-all duration-300 group-hover:scale-y-100 group-hover:opacity-100"
            />
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 pl-0 text-sm text-mist transition-[padding] duration-300 group-hover:pl-3">
              <time>{formatDate(post.publishedAt)}</time>
              <span className="text-gold/70" aria-hidden>
                ·
              </span>
              <span>{post.readingTimeMinutes} 分钟</span>
              {post.categoryName ? (
                <>
                  <span className="text-gold/70" aria-hidden>
                    ·
                  </span>
                  <span>{post.categoryName}</span>
                </>
              ) : null}
            </div>
            <h2 className="mt-3 font-serif text-2xl leading-snug tracking-wide text-ink transition-colors duration-200 group-hover:text-seal sm:text-[1.75rem]">
              {post.title}
            </h2>
            {post.summary ? (
              <p className="mt-3 max-w-2xl text-[1.02rem] leading-7 text-mist line-clamp-2">
                {post.summary}
              </p>
            ) : null}
          </Link>
        </li>
      ))}
    </ul>
  );
}
