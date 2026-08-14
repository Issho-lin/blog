import Link from "next/link";

export function Pagination({
  page,
  totalPages,
  hrefFor,
}: {
  page: number;
  totalPages: number;
  hrefFor: (page: number) => string;
}) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav
      className="mt-10 flex items-center justify-between text-sm text-mist"
      aria-label="分页"
    >
      {page > 1 ? (
        <Link
          href={hrefFor(page - 1)}
          className="cursor-pointer transition-colors duration-200 hover:text-seal"
        >
          上一页
        </Link>
      ) : (
        <span />
      )}
      <span>
        {page} / {totalPages}
      </span>
      {page < totalPages ? (
        <Link
          href={hrefFor(page + 1)}
          className="cursor-pointer transition-colors duration-200 hover:text-seal"
        >
          下一页
        </Link>
      ) : (
        <span />
      )}
    </nav>
  );
}
