import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { Pagination } from "@/components/Pagination";
import { PostList } from "@/components/PostList";
import { ApiError } from "@/lib/api/client";
import { getPublicTag, listPublishedPosts } from "@/lib/api/posts";
import type { PublicPostSummary } from "@/lib/api/types";

type PageProps = {
  params: Promise<{ slug: string }>;
  searchParams: Promise<{ page?: string }>;
};

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  try {
    const tag = await getPublicTag(slug);
    return {
      title: `#${tag.name}`,
      description: `标签：${tag.name}`,
    };
  } catch {
    return { title: "标签未找到" };
  }
}

export default async function TagDetailPage({ params, searchParams }: PageProps) {
  const { slug } = await params;
  const { page: pageParam } = await searchParams;
  const page = Math.max(1, Number(pageParam) || 1);

  try {
    const tag = await getPublicTag(slug);
    const result = await listPublishedPosts(page, 10, { tagId: tag.id });
    const posts: PublicPostSummary[] = result.items;

    return (
      <PublicShell>
        <PageIntro
          eyebrow="鈐印"
          title={`#${tag.name}`}
          description="盖有这枚标签的已发布文章。"
        />
        <section className="pt-12">
          <div className="mb-6 flex items-baseline justify-between gap-4">
            <h2 className="font-serif text-xl tracking-[0.12em] text-ink">篇目</h2>
            <span className="text-sm tracking-wider text-mist">{result.total} 篇</span>
          </div>
          <PostList posts={posts} emptyText="这个标签下还没有已发布文章。" />
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            hrefFor={(nextPage) =>
              nextPage === 1 ? `/tags/${slug}` : `/tags/${slug}?page=${nextPage}`
            }
          />
        </section>
      </PublicShell>
    );
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
