"use client";

import { useEffect, useState } from "react";
import { absoluteUrl, copyText } from "@/lib/clipboard";

type ArticleShareActionsProps = {
  title: string;
  text?: string | null;
  path: string;
};

export function ArticleShareActions({ title, text, path }: ArticleShareActionsProps) {
  const [copied, setCopied] = useState(false);
  const [canShare, setCanShare] = useState(false);
  const [shareHint, setShareHint] = useState<string | null>(null);

  useEffect(() => {
    setCanShare(typeof navigator !== "undefined" && typeof navigator.share === "function");
  }, []);

  useEffect(() => {
    if (!copied) return;
    const timer = window.setTimeout(() => setCopied(false), 2000);
    return () => window.clearTimeout(timer);
  }, [copied]);

  async function onCopyLink() {
    const ok = await copyText(absoluteUrl(path));
    setCopied(ok);
    setShareHint(ok ? null : "复制失败");
  }

  async function onShare() {
    const url = absoluteUrl(path);
    try {
      await navigator.share({
        title,
        text: text?.trim() || title,
        url,
      });
      setShareHint(null);
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") {
        return;
      }
      const ok = await copyText(url);
      setCopied(ok);
      setShareHint(ok ? null : "分享失败");
    }
  }

  return (
    <div className="print-hide flex flex-wrap items-center gap-4 text-sm">
      <button
        type="button"
        className="cursor-pointer text-mist transition-colors duration-200 hover:text-seal"
        onClick={() => void onCopyLink()}
      >
        {copied ? "链接已复制" : "复制链接"}
      </button>
      {canShare ? (
        <button
          type="button"
          className="cursor-pointer text-mist transition-colors duration-200 hover:text-seal"
          onClick={() => void onShare()}
        >
          分享
        </button>
      ) : null}
      {shareHint ? <span className="text-warn">{shareHint}</span> : null}
    </div>
  );
}
