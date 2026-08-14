import Link from "next/link";
import type { ReactNode } from "react";

const categoryClass =
  "inline-flex max-w-full items-center gap-1.5 truncate rounded-sm border border-seal/15 bg-seal-soft px-2.5 py-1 text-sm tracking-wide text-seal";

const tagClass =
  "inline-flex max-w-full items-center truncate rounded-full border border-line px-3 py-1 text-sm text-mist";

function wrap(href: string | undefined, className: string, hover: string, children: ReactNode) {
  if (!href) {
    return <span className={className}>{children}</span>;
  }
  return (
    <Link href={href} className={`${className} cursor-pointer transition-colors duration-200 ${hover}`}>
      {children}
    </Link>
  );
}

export function CategoryMark({
  name,
  href,
}: {
  name: string;
  href?: string;
}) {
  return wrap(
    href,
    categoryClass,
    "hover:border-seal/40 hover:bg-seal hover:text-paper",
    <>
      <span className="shrink-0 text-[0.65rem] tracking-[0.28em] opacity-70">卷</span>
      <span className="truncate">{name}</span>
    </>
  );
}

export function TagMark({
  name,
  href,
}: {
  name: string;
  href?: string;
}) {
  return wrap(
    href,
    tagClass,
    "hover:border-gold/50 hover:text-ink",
    <>#{name}</>
  );
}

export function TaxonomyRow({
  categoryName,
  categoryHref,
  tags,
}: {
  categoryName?: string | null;
  categoryHref?: string | null;
  tags: { name: string; href?: string | null }[];
}) {
  if (!categoryName && tags.length === 0) return null;

  return (
    <div className="flex flex-wrap items-center gap-2">
      {categoryName ? (
        <CategoryMark name={categoryName} href={categoryHref ?? undefined} />
      ) : null}
      {tags.map((tag) => (
        <TagMark key={tag.href ?? tag.name} name={tag.name} href={tag.href ?? undefined} />
      ))}
    </div>
  );
}
