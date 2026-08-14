import type { Metadata } from "next";
import Link from "next/link";
import { PageIntro, PublicShell } from "@/components/PublicShell";
import { ApiError } from "@/lib/api/client";
import { getArchives } from "@/lib/api/posts";
import type { ArchiveGroup } from "@/lib/api/types";

export const metadata: Metadata = {
  title: "归档",
  description: "按年月浏览已发布文章。",
};

const monthNames = [
  "一月",
  "二月",
  "三月",
  "四月",
  "五月",
  "六月",
  "七月",
  "八月",
  "九月",
  "十月",
  "十一月",
  "十二月",
];

function nestByYear(groups: ArchiveGroup[]) {
  const years: { year: number; months: ArchiveGroup[] }[] = [];
  for (const group of groups) {
    const last = years[years.length - 1];
    if (last && last.year === group.year) {
      last.months.push(group);
    } else {
      years.push({ year: group.year, months: [group] });
    }
  }
  return years;
}

function formatDay(value: string | null) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(value));
}

export default async function ArchivePage() {
  let groups: ArchiveGroup[] = [];
  let loadError: string | null = null;

  try {
    groups = await getArchives();
  } catch (error) {
    loadError =
      error instanceof ApiError
        ? error.message
        : "暂时无法连接后端，请确认后端已启动。";
  }

  const years = nestByYear(groups);

  return (
    <PublicShell>
      <PageIntro
        eyebrow="目錄 · 年月"
        title="归档"
        description="按落笔时间翻阅，像在书架上抽出一卷旧稿。"
      />

      {loadError ? (
        <div className="mt-10 border border-warn/25 bg-seal-soft/60 px-4 py-5 text-sm text-warn">
          {loadError}
        </div>
      ) : years.length === 0 ? (
        <p className="mt-12 border-y border-line py-12 text-mist">
          还没有可归档的文章。
        </p>
      ) : (
        <div className="mt-12 space-y-16">
          {years.map((yearGroup) => (
            <section key={yearGroup.year} className="soft-in">
              <h2 className="font-serif text-4xl tracking-[0.12em] text-ink">
                {yearGroup.year}
              </h2>
              <span className="gold-rule mt-4" aria-hidden />
              <div className="mt-8 space-y-10">
                {yearGroup.months.map((month) => (
                  <div key={`${month.year}-${month.month}`}>
                    <h3 className="mb-4 text-sm tracking-[0.28em] text-gold">
                      {monthNames[month.month - 1] ?? `${month.month} 月`}
                    </h3>
                    <ul className="divide-y divide-line border-y border-line">
                      {month.items.map((item) => (
                        <li key={item.slug}>
                          <Link
                            href={`/posts/${item.slug}`}
                            className="group flex cursor-pointer items-baseline justify-between gap-6 py-4"
                          >
                            <span className="font-serif text-lg tracking-wide text-ink transition-colors duration-200 group-hover:text-seal">
                              {item.title}
                            </span>
                            <time className="shrink-0 text-sm tabular-nums text-mist">
                              {formatDay(item.publishedAt)}
                            </time>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </PublicShell>
  );
}
