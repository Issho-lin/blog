const CJK = /[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF]/;
const CJK_GLOBAL = /[\u4E00-\u9FFF\u3400-\u4DBF\uF900-\uFAFF]/g;
const WORDS_PER_MINUTE = 200;

/**
 * 与后端 SummaryGenerator.stripMarkdown / ReadingTimeEstimator 对齐，
 * 保证编辑器字数、时长和公开页一致。
 */
export function stripMarkdown(markdown: string): string {
  if (!markdown.trim()) return "";
  return markdown
    .replace(/^#{1,6}\s+/gm, "")
    .replace(/!\[([^\]]*)]\([^)]*\)/g, "$1")
    .replace(/\[([^\]]*)]\([^)]*\)/g, "$1")
    .replace(/\*\*(.+?)\*\*/g, "$1")
    .replace(/\*(.+?)\*/g, "$1")
    .replace(/__(.+?)__/g, "$1")
    .replace(/_(.+?)_/g, "$1")
    .replace(/~~(.+?)~~/g, "$1")
    .replace(/```[\s\S]*?```/g, "")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/^>\s+/gm, "")
    .replace(/^[\-*+]\s+/gm, "")
    .replace(/^\d+\.\s+/gm, "")
    .replace(/^-{3,}\s*$/gm, "")
    .replace(/<[^>]+>/g, "")
    .replace(/\n{2,}/g, "\n")
    .trim();
}

export function countWords(plainText: string): number {
  if (!plainText.trim()) return 0;
  const trimmed = plainText.trim();
  const cjkCount = [...trimmed].filter((char) => CJK.test(char)).length;
  const nonCjkText = trimmed.replace(CJK_GLOBAL, " ");
  const nonCjkWordCount = nonCjkText.split(/\s+/).filter((word) => word.length > 0).length;
  return cjkCount + nonCjkWordCount;
}

export function estimateReadingMinutes(plainText: string): number {
  const words = countWords(plainText);
  if (words <= 0) return 1;
  return Math.max(1, Math.ceil(words / WORDS_PER_MINUTE));
}

export function readingStatsFromMarkdown(markdown: string) {
  const plainText = stripMarkdown(markdown ?? "");
  return {
    words: countWords(plainText),
    minutes: estimateReadingMinutes(plainText),
  };
}
