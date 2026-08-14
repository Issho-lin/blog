import type { Metadata } from "next";
import Link from "next/link";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { ApiError } from "@/lib/api/client";
import { listPublicCategories } from "@/lib/api/posts";
import type { Category } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "分类",
  description: "按主题浏览文章。",
};

export default async function CategoriesPage() {
  let categories: Category[] = [];
  let loadError: string | null = null;

  try {
    categories = await listPublicCategories();
  } catch (error) {
    loadError =
      error instanceof ApiError
        ? error.message
        : "暂时无法连接后端，请确认后端已启动。";
  }

  return (
    <PublicShell>
      <PageIntro
        eyebrow="分卷"
        title="分类"
        description="文章按主题归入不同卷册，点开即可阅读该卷下的篇目。"
      />

      {loadError ? (
        <div className="mt-10 border border-warn/25 bg-seal-soft/60 px-4 py-5 text-sm text-warn">
          {loadError}
        </div>
      ) : categories.length === 0 ? (
        <p className="mt-12 border-y border-line py-12 text-mist">
          还没有分类。可在管理台编辑文章时指定。
        </p>
      ) : (
        <ul className="mt-12 grid gap-4 sm:grid-cols-2">
          {categories.map((category) => (
            <li key={category.id}>
              <Link
                href={`/categories/${category.slug}`}
                className="group block cursor-pointer rounded-sm border border-line bg-white/55 px-5 py-6 transition-colors duration-200 hover:border-seal/30"
              >
                <p className="text-[0.65rem] tracking-[0.32em] text-gold">卷</p>
                <h2 className="mt-2 font-serif text-2xl tracking-wide text-ink transition-colors duration-200 group-hover:text-seal">
                  {category.name}
                </h2>
                {category.description ? (
                  <p className="mt-2 line-clamp-2 text-sm leading-6 text-mist">
                    {category.description}
                  </p>
                ) : (
                  <p className="mt-2 text-sm text-mist/70">点开阅读这一卷的篇目</p>
                )}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </PublicShell>
  );
}
