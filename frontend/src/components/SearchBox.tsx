"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export function SearchBox({
  defaultQuery = "",
  autoFocus = false,
}: {
  defaultQuery?: string;
  autoFocus?: boolean;
}) {
  const router = useRouter();
  const [query, setQuery] = useState(defaultQuery);

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const keyword = query.trim();
    if (!keyword) {
      router.push("/search");
      return;
    }
    router.push(`/search?q=${encodeURIComponent(keyword)}`);
  }

  return (
    <form
      onSubmit={onSubmit}
      className="flex items-center border-b border-line transition-colors duration-200 focus-within:border-gold"
    >
      <label className="min-w-0 flex-1">
        <span className="sr-only">搜索文章</span>
        <input
          type="search"
          name="q"
          value={query}
          autoFocus={autoFocus}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="查找标题或正文"
          autoComplete="off"
          className="search-field w-full py-3 text-base outline-none placeholder:text-mist/80"
        />
      </label>
      <button
        type="submit"
        className="cursor-pointer py-3 pl-4 text-sm text-seal transition-colors duration-200 hover:text-ink"
      >
        搜索
      </button>
    </form>
  );
}
