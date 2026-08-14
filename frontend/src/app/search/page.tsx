import type { Metadata } from "next";
import Link from "next/link";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { Pagination } from "@/components/Pagination";
import { PostList } from "@/components/PostList";
import { SearchBox } from "@/components/SearchBox";
import { TagMark } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import { listPublicTags, searchPublishedPosts } from "@/lib/api/posts";
import type { PublicPostSummary, Tag } from "@/lib/api/types";

type PageProps = {
  searchParams: Promise<{ q?: string; page?: string }>;
};

export async function generateMetadata({ searchParams }: PageProps): Promise<Metadata> {
  const { q } = await searchParams;
  const keyword = q?.trim();
  return {
    title: keyword ? `搜索「${keyword}」` : "搜索",
    description: "按标题或正文搜索已发布文章。",
  };
}

export default async function SearchPage({ searchParams }: PageProps) {
  const { q, page: pageParam } = await searchParams;
  const keyword = q?.trim() ?? "";
  const page = Math.max(1, Number(pageParam) || 1);

  let posts: PublicPostSummary[] = [];
  let total = 0;
  let totalPages = 0;
  let currentPage = page;
  let loadError: string | null = null;
  let tags: Tag[] = [];

  if (keyword) {
    try {
      const result = await searchPublishedPosts(keyword, page, 10);
      posts = result.items;
      total = result.total;
      totalPages = result.totalPages;
      currentPage = result.page;
    } catch (error) {
      loadError =
        error instanceof ApiError
          ? error.message
          : "暂时无法连接后端，请确认后端已启动。";
    }
  } else {
    try {
      tags = await listPublicTags();
    } catch {
      tags = [];
    }
  }

  return (
    <PublicShell>
      <PageIntro eyebrow="檢索" title="搜索">
        <SearchBox key={keyword} defaultQuery={keyword} autoFocus={!keyword} />
      </PageIntro>

      {keyword ? (
        <section className="pt-12">
          <div className="mb-6 flex items-baseline justify-between gap-4">
            <h2 className="font-serif text-xl tracking-[0.12em] text-ink">
              「{keyword}」
            </h2>
            {!loadError ? (
              <span className="text-sm tracking-wider text-mist">{total} 篇</span>
            ) : null}
          </div>
          {loadError ? (
            <div className="border border-warn/25 bg-seal-soft/60 px-4 py-5 text-sm text-warn">
              {loadError}
            </div>
          ) : (
            <>
              <PostList posts={posts} emptyText={`没有找到与「${keyword}」相关的文章。`} />
              <Pagination
                page={currentPage}
                totalPages={totalPages}
                hrefFor={(nextPage) => {
                  const params = new URLSearchParams({ q: keyword });
                  if (nextPage > 1) {
                    params.set("page", String(nextPage));
                  }
                  return `/search?${params.toString()}`;
                }}
              />
            </>
          )}
        </section>
      ) : tags.length > 0 ? (
        <section className="pt-12">
          <h2 className="mb-5 font-serif text-xl tracking-[0.12em] text-ink">标签</h2>
          <ul className="flex flex-wrap gap-2">
            {tags.map((tag) => (
              <li key={tag.id}>
                <TagMark name={tag.name} href={`/tags/${tag.slug}`} />
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </PublicShell>
  );
}
