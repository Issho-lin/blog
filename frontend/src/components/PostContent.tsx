"use client";

import { useEffect, useRef } from "react";
import { applyMermaidConfig } from "@/lib/mermaid";

/**
 * 渲染文章 HTML，并将 ```mermaid 代码块画成图。
 */
export function PostContent({ html }: { html: string }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const root = ref.current;
    if (!root) return;

    let cancelled = false;

    async function renderMermaid() {
      const codes = root!.querySelectorAll("pre code.language-mermaid");
      if (codes.length === 0) return;

      const mermaid = (await import("mermaid")).default;
      if (cancelled) return;

      applyMermaidConfig(mermaid);

      const nodes: HTMLElement[] = [];
      codes.forEach((code) => {
        const pre = code.parentElement;
        if (!pre || !(pre instanceof HTMLElement)) return;
        if (pre.dataset.mermaidReady === "true") return;

        const source = code.textContent ?? "";
        const wrap = document.createElement("div");
        wrap.className = "mermaid-diagram";
        const graph = document.createElement("div");
        graph.className = "mermaid";
        graph.textContent = source;
        wrap.appendChild(graph);
        pre.replaceWith(wrap);
        nodes.push(graph);
      });

      if (nodes.length === 0) return;
      try {
        await mermaid.run({ nodes });
      } catch {
        // 单图语法错误不阻断整页
      }
    }

    void renderMermaid();
    return () => {
      cancelled = true;
    };
  }, [html]);

  return (
    <div
      ref={ref}
      className="prose-blog soft-in-delay mt-10"
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
