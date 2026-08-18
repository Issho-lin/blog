"use client";

import { useEffect, useRef } from "react";
import { copyText } from "@/lib/clipboard";
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

    function enhanceCopyButtons() {
      const blocks = root.querySelectorAll("pre");
      blocks.forEach((pre) => {
        if (!(pre instanceof HTMLElement)) return;
        if (pre.querySelector("code.language-mermaid")) return;
        if (pre.closest(".code-block")) return;

        const wrap = document.createElement("div");
        wrap.className = "code-block";
        const button = document.createElement("button");
        button.type = "button";
        button.className = "code-copy print-hide";
        button.textContent = "复制";
        button.addEventListener("click", () => {
          const code = pre.querySelector("code");
          const source = (code?.textContent ?? pre.textContent ?? "").replace(/\n$/, "");
          void copyText(source).then((ok) => {
            button.textContent = ok ? "已复制" : "复制失败";
            window.setTimeout(() => {
              button.textContent = "复制";
            }, 2000);
          });
        });
        pre.parentNode?.insertBefore(wrap, pre);
        wrap.appendChild(button);
        wrap.appendChild(pre);
      });
    }

    void renderMermaid().then(() => {
      if (!cancelled) enhanceCopyButtons();
    });
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
