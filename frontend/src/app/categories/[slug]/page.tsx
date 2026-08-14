import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { Pagination } from "@/components/Pagination";
import { PostList } from "@/components/PostList";
import { ApiError } from "@/lib/api/client";
import { getPublicCategory, listPublishedPosts } from "@/lib/api/posts";
import type { PublicPostSummary } from "@/lib/api/types";

type PageProps = {
  params: Promise<{ slug: string }>;
  searchParams: Promise<{ page?: string }>;
};

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { slug } = await params;
  try {
    const category = await getPublicCategory(slug);
    return {
      title: category.name,
      description: category.description ?? `分类：${category.name}`,
    };
  } catch {
    return { title: "分类未找到" };
  }
}

export default async function CategoryDetailPage({ params, searchParams }: PageProps) {
  const { slug } = await params;
  const { page: pageParam } = await searchParams;
  const page = Math.max(1, Number(pageParam) || 1);

  try {
    const category = await getPublicCategory(slug);
    const result = await listPublishedPosts(page, 10, { categoryId: category.id });
    const posts: PublicPostSummary[] = result.items;

    return (
      <PublicShell>
        <PageIntro
          eyebrow="分卷"
          title={category.name}
          description={category.description ?? "这一卷里的已发布文章。"}
        />
        <section className="pt-12">
          <div className="mb-6 flex items-baseline justify-between gap-4">
            <h2 className="font-serif text-xl tracking-[0.12em] text-ink">篇目</h2>
            <span className="text-sm tracking-wider text-mist">{result.total} 篇</span>
          </div>
          <PostList posts={posts} emptyText="这个分类下还没有已发布文章。" />
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            hrefFor={(nextPage) =>
              nextPage === 1
                ? `/categories/${slug}`
                : `/categories/${slug}?page=${nextPage}`
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
