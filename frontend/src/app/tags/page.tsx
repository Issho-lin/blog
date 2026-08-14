import type { Metadata } from "next";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { TagMark } from "@/components/TaxonomyMarks";
import { ApiError } from "@/lib/api/client";
import { listPublicTags } from "@/lib/api/posts";
import type { Tag } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "标签",
  description: "按标签浏览文章。",
};

export default async function TagsPage() {
  let tags: Tag[] = [];
  let loadError: string | null = null;

  try {
    tags = await listPublicTags();
  } catch (error) {
    loadError =
      error instanceof ApiError
        ? error.message
        : "暂时无法连接后端，请确认后端已启动。";
  }

  return (
    <PublicShell>
      <PageIntro
        eyebrow="鈐印"
        title="标签"
        description="比分类更细的记号。一篇文章可以盖上多枚。"
      />

      {loadError ? (
        <div className="mt-10 border border-warn/25 bg-seal-soft/60 px-4 py-5 text-sm text-warn">
          {loadError}
        </div>
      ) : tags.length === 0 ? (
        <p className="mt-12 border-y border-line py-12 text-mist">
          还没有标签。可在编辑文章时添加。
        </p>
      ) : (
        <ul className="mt-12 flex flex-wrap gap-2.5">
          {tags.map((tag) => (
            <li key={tag.id}>
              <TagMark name={tag.name} href={`/tags/${tag.slug}`} />
            </li>
          ))}
        </ul>
      )}
    </PublicShell>
  );
}
