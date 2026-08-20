"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { FormEvent, useEffect, useId, useRef, useState } from "react";
import { SealMark } from "@/components/SealMark";
import { ApiError } from "@/lib/api/client";
import { getPublicAiStatus, streamPublicAiChat } from "@/lib/api/posts";

type ChatItem = {
  role: "user" | "assistant";
  content: string;
  citations?: Array<{ title: string; url: string }>;
};

const SESSION_KEY = "blog-ai-session";

const STARTERS = ["最近在写什么？", "有哪些适合入门的文章？", "帮我找一篇工程实践相关的"];

function sessionId(): string {
  const existing = window.localStorage.getItem(SESSION_KEY);
  if (existing) return existing;
  const created = crypto.randomUUID();
  window.localStorage.setItem(SESSION_KEY, created);
  return created;
}

export function SiteAssistant() {
  const pathname = usePathname();
  const titleId = useId();
  const inputId = useId();
  const transcriptRef = useRef<HTMLDivElement>(null);
  const [enabled, setEnabled] = useState(false);
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatItem[]>([]);

  useEffect(() => {
    if (pathname.startsWith("/admin")) return;
    let cancelled = false;
    getPublicAiStatus()
      .then((status) => {
        if (!cancelled) setEnabled(status.assistantEnabled);
      })
      .catch(() => {
        if (!cancelled) setEnabled(false);
      });
    return () => {
      cancelled = true;
    };
  }, [pathname]);

  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);

  useEffect(() => {
    const node = transcriptRef.current;
    if (!node) return;
    node.scrollTop = node.scrollHeight;
  }, [messages, busy, error, open]);

  if (pathname.startsWith("/admin") || !enabled) {
    return null;
  }

  async function send(text: string) {
    if (!text || busy) return;
    const history: ChatItem[] = [...messages, { role: "user", content: text }];
    setMessages([...history, { role: "assistant", content: "" }]);
    setInput("");
    setBusy(true);
    setError(null);
    try {
      await streamPublicAiChat(
        sessionId(),
        history.map((item) => ({ role: item.role, content: item.content })),
        (event, data) => {
          if (event === "delta") {
            const piece = typeof data.text === "string" ? data.text : "";
            if (!piece) return;
            setMessages((current) => {
              const updated = [...current];
              const last = updated[updated.length - 1];
              if (!last || last.role !== "assistant") return current;
              updated[updated.length - 1] = { ...last, content: last.content + piece };
              return updated;
            });
            return;
          }
          if (event === "meta") {
            const raw = Array.isArray(data.citations) ? data.citations : [];
            const citations = raw.flatMap((item) => {
              if (!item || typeof item !== "object") return [];
              const record = item as { title?: unknown; metadata?: { url?: unknown } };
              const title = typeof record.title === "string" ? record.title : "";
              const url =
                record.metadata && typeof record.metadata.url === "string" ? record.metadata.url : "";
              if (!title && !url) return [];
              return [{ title, url }];
            });
            setMessages((current) => {
              const updated = [...current];
              const last = updated[updated.length - 1];
              if (!last || last.role !== "assistant") return current;
              updated[updated.length - 1] = { ...last, citations };
              return updated;
            });
            return;
          }
          if (event === "error") {
            const message = typeof data.message === "string" ? data.message : "助手暂时不可用";
            setError(message);
          }
        }
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "助手暂时不可用");
    } finally {
      setBusy(false);
    }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void send(input.trim());
  }

  return (
    <div className="site-assistant print-hide">
      {open ? (
        <div
          className="site-assistant-sheet"
          role="dialog"
          aria-modal="true"
          aria-labelledby={titleId}
        >
          <div className="site-assistant-frame" aria-hidden="true" />
          <header className="site-assistant-head">
            <div className="flex min-w-0 items-center gap-2.5">
              <SealMark size={28} className="shrink-0 text-seal" />
              <div className="min-w-0">
                <h2 id={titleId} className="font-serif text-[1.05rem] tracking-[0.12em] text-ink">
                  问文章
                </h2>
                <p className="mt-0.5 text-[11px] tracking-[0.18em] text-mist">已发布 · 可引用原文</p>
              </div>
            </div>
            <button
              type="button"
              className="site-assistant-icon-btn"
              onClick={() => setOpen(false)}
              aria-label="关闭助手"
            >
              <CloseIcon />
            </button>
          </header>
          <div className="gold-rule mx-5 w-auto max-w-none" />
          <div ref={transcriptRef} className="site-assistant-transcript">
            {messages.length === 0 ? (
              <div className="site-assistant-empty">
                <p className="font-serif text-[0.95rem] leading-relaxed text-ink">
                  闲聊可以，问站内已写过的文章更好。
                </p>
                <p className="mt-2 text-xs leading-relaxed text-mist">
                  回答会尽量落到原文，并在文末给出链接。
                </p>
                <ul className="mt-4 flex flex-col gap-2">
                  {STARTERS.map((prompt) => (
                    <li key={prompt}>
                      <button
                        type="button"
                        className="site-assistant-chip"
                        disabled={busy}
                        onClick={() => void send(prompt)}
                      >
                        {prompt}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
            {messages.map((item, index) => (
              <article
                key={`${item.role}-${index}`}
                className={item.role === "user" ? "site-assistant-turn is-user" : "site-assistant-turn is-bot"}
              >
                <span className="site-assistant-role" aria-hidden="true">
                  {item.role === "user" ? "我" : "林"}
                </span>
                <div className="site-assistant-bubble">
                  <p className="whitespace-pre-wrap">
                    {item.content}
                    {busy && item.role === "assistant" && index === messages.length - 1 && item.content ? (
                      <span className="site-assistant-caret" aria-hidden="true">
                        ▍
                      </span>
                    ) : null}
                  </p>
                  {item.citations && item.citations.length > 0 ? (
                    <ul className="site-assistant-cites">
                      {item.citations.map((citation) => (
                        <li key={`${citation.url}-${citation.title}`}>
                          {citation.url ? (
                            <Link href={citation.url} className="hover:text-seal">
                              {citation.title || citation.url}
                            </Link>
                          ) : (
                            citation.title
                          )}
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              </article>
            ))}
            {busy && !messages.at(-1)?.content ? (
              <p className="site-assistant-thinking">
                <span />
                <span />
                <span />
                <span className="sr-only">正在思考</span>
              </p>
            ) : null}
            {error ? <p className="site-assistant-error">{error}</p> : null}
          </div>
          <form className="site-assistant-composer" onSubmit={onSubmit}>
            <label className="sr-only" htmlFor={inputId}>
              提问
            </label>
            <div className="site-assistant-field">
              <input
                id={inputId}
                value={input}
                onChange={(event) => setInput(event.target.value)}
                className="site-assistant-input"
                placeholder="问一篇文章，或随便聊聊"
                maxLength={2000}
                autoComplete="off"
                disabled={busy}
              />
              <button
                type="submit"
                disabled={busy || !input.trim()}
                className="site-assistant-send"
                aria-label="发送"
              >
                <SendIcon />
              </button>
            </div>
          </form>
        </div>
      ) : null}
      <button
        type="button"
        className="site-assistant-launch"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-label={open ? "收起问文章助手" : "打开问文章助手"}
      >
        <span className="site-assistant-launch-seal" aria-hidden="true">
          <svg viewBox="0 0 64 64" fill="none">
            <rect x="3" y="3" width="58" height="58" rx="4" stroke="currentColor" strokeWidth="3.5" />
            <rect
              x="9"
              y="9"
              width="46"
              height="46"
              rx="2"
              stroke="currentColor"
              strokeWidth="1.25"
              opacity="0.55"
            />
          </svg>
          <span className="site-assistant-launch-glyph">问</span>
        </span>
      </button>
    </div>
  );
}

function SendIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M3.4 11.2 20.2 4.6c.7-.3 1.4.4 1.1 1.1l-6.6 16.8c-.3.7-1.3.7-1.6 0l-2.4-6.2-6.2-2.4c-.7-.3-.7-1.3 0-1.7Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <path d="M10.7 13.3 21 5.8" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <path d="M3.2 3.2l9.6 9.6M12.8 3.2l-9.6 9.6" stroke="currentColor" strokeWidth="1.4" />
    </svg>
  );
}
