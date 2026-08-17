export type EditorBackup = {
  postId: string;
  savedAt: number;
  baseVersion: number;
  title: string;
  slug: string;
  markdown: string;
  excerpt: string;
  coverUrl: string;
  categoryId: string;
  tagIds: string[];
};

const PREFIX = "blog.editor.backup.";

function keyFor(postId: string) {
  return `${PREFIX}${postId}`;
}

export function readEditorBackup(postId: string): EditorBackup | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(keyFor(postId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as EditorBackup;
    if (!parsed || parsed.postId !== postId) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function writeEditorBackup(backup: EditorBackup) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(keyFor(backup.postId), JSON.stringify(backup));
  } catch {
    // 配额满或隐私模式：忽略，不阻断编辑
  }
}

export function clearEditorBackup(postId: string) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(keyFor(postId));
  } catch {
    /* ignore */
  }
}

export function backupMatchesPost(
  backup: EditorBackup,
  post: {
    title: string;
    slug: string;
    markdownContent: string;
    excerpt: string | null;
    coverUrl: string | null;
    categoryId: string | null;
    tagIds: string[] | null;
  }
) {
  const tags = [...(post.tagIds ?? [])].sort().join(",");
  const backupTags = [...backup.tagIds].sort().join(",");
  return (
    backup.title === post.title &&
    backup.slug === post.slug &&
    backup.markdown === (post.markdownContent ?? "") &&
    backup.excerpt === (post.excerpt ?? "") &&
    backup.coverUrl === (post.coverUrl ?? "") &&
    backup.categoryId === (post.categoryId ?? "") &&
    backupTags === tags
  );
}

export function isLikelyOffline(error: unknown) {
  if (typeof navigator !== "undefined" && navigator.onLine === false) return true;
  if (error instanceof TypeError) return true;
  return false;
}
