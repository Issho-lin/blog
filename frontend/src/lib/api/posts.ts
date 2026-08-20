import { ApiError, apiRequest, apiUpload, downloadFile } from "./client";
import type {
  AdminPost,
  ArchiveGroup,
  AuthUser,
  AdminDashboard,
  BatchPostActionResult,
  Category,
  ImportPostResult,
  MarkdownPreview,
  PageResponse,
  PublicPostDetail,
  PublicPostSummary,
  AdminPostPreview,
  SiteSettings,
  Tag,
  UpdatePostInput,
  PublicComment,
  AdminComment,
  PostRevisionSummary,
  PostRevisionDetail,
  AiTextResult,
  AiTaxonomyResult,
  AiSettings,
  PublicAiStatus,
  PublicAiChatResult,
} from "./types";

export function listPublishedPosts(
  page = 1,
  pageSize = 10,
  filters?: { categoryId?: string; tagId?: string }
) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  if (filters?.categoryId) {
    params.set("categoryId", filters.categoryId);
  }
  if (filters?.tagId) {
    params.set("tagId", filters.tagId);
  }
  return apiRequest<PageResponse<PublicPostSummary>>(
    `/api/public/posts?${params.toString()}`
  );
}

export function searchPublishedPosts(keyword: string, page = 1, pageSize = 10) {
  const params = new URLSearchParams({
    q: keyword,
    page: String(page),
    pageSize: String(pageSize),
  });
  return apiRequest<PageResponse<PublicPostSummary>>(
    `/api/public/posts/search?${params.toString()}`
  );
}

export function getArchives() {
  return apiRequest<ArchiveGroup[]>("/api/public/posts/archives");
}

export function listPublicCategories() {
  return apiRequest<Category[]>("/api/public/categories");
}

export function getPublicCategory(slug: string) {
  return apiRequest<Category>(
    `/api/public/categories/${encodeURIComponent(slug)}`
  );
}

export function listPublicTags() {
  return apiRequest<Tag[]>("/api/public/tags");
}

export function getPublicTag(slug: string) {
  return apiRequest<Tag>(`/api/public/tags/${encodeURIComponent(slug)}`);
}

export function getPublishedPost(slug: string) {
  return apiRequest<PublicPostDetail>(
    `/api/public/posts/${encodeURIComponent(slug)}`
  );
}

export function login(email: string, password: string) {
  return apiRequest<AuthUser>("/api/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export function logout() {
  return apiRequest<null>("/api/auth/logout", { method: "POST" });
}

export function getCurrentUser() {
  return apiRequest<AuthUser>("/api/auth/me");
}

export function getAdminDashboard() {
  return apiRequest<AdminDashboard>("/api/admin/dashboard");
}

export function listAdminPosts(filters?: {
  keyword?: string;
  categoryId?: string;
  tagId?: string;
}) {
  const params = new URLSearchParams();
  if (filters?.keyword) {
    params.set("keyword", filters.keyword);
  }
  if (filters?.categoryId) {
    params.set("categoryId", filters.categoryId);
  }
  if (filters?.tagId) {
    params.set("tagId", filters.tagId);
  }
  const query = params.toString() ? `?${params.toString()}` : "";
  return apiRequest<AdminPost[]>(`/api/admin/posts${query}`);
}

export function getAdminPost(postId: string) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}`);
}

export function getAdminPostPreview(postId: string) {
  return apiRequest<AdminPostPreview>(`/api/admin/posts/${postId}/preview`);
}

export function createDraft(title: string, markdownContent = "") {
  return apiRequest<AdminPost>("/api/admin/posts/drafts", {
    method: "POST",
    body: { title, markdownContent },
  });
}

export function updatePost(postId: string, input: UpdatePostInput) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}`, {
    method: "PUT",
    body: input,
  });
}

export function summarizeWithAi(markdown: string) {
  return apiRequest<AiTextResult>("/api/admin/ai/summarize", {
    method: "POST",
    body: { markdown },
  });
}

export function writeWithAi(input: {
  markdown: string;
  instruction?: string;
  mode?: string;
}) {
  return apiRequest<AiTextResult>("/api/admin/ai/write", {
    method: "POST",
    body: input,
  });
}

export function suggestTaxonomyWithAi(title: string, markdown: string) {
  return apiRequest<AiTaxonomyResult>("/api/admin/ai/taxonomy", {
    method: "POST",
    body: { title, markdown },
  });
}

export function getAdminAiSettings() {
  return apiRequest<AiSettings>("/api/admin/ai/settings");
}

export function updateAdminAiSettings(
  payload: Omit<AiSettings, "chatApiKeyConfigured" | "embedApiKeyConfigured" | "updatedAt"> & {
    chatApiKey?: string;
    embedApiKey?: string;
  }
) {
  return apiRequest<AiSettings>("/api/admin/ai/settings", {
    method: "PUT",
    body: payload,
  });
}

export function rebuildAiIndex() {
  return apiRequest<{ indexed: number }>("/api/admin/ai/index/rebuild", {
    method: "POST",
  });
}

export function getPublicAiStatus() {
  return apiRequest<PublicAiStatus>("/api/public/ai/status");
}

export function chatWithPublicAi(
  sessionId: string,
  messages: Array<{ role: "user" | "assistant"; content: string }>
) {
  return apiRequest<PublicAiChatResult>("/api/public/ai/chat", {
    method: "POST",
    body: { sessionId, messages },
  });
}

export async function streamPublicAiChat(
  sessionId: string,
  messages: Array<{ role: "user" | "assistant"; content: string }>,
  onEvent: (event: "meta" | "delta" | "done" | "error", data: Record<string, unknown>) => void
) {
  const configured = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();
  const baseUrl =
    configured?.replace(/\/$/, "") ??
    (typeof window === "undefined"
      ? (process.env.API_PROXY_ORIGIN?.replace(/\/$/, "") ?? "http://localhost:8080")
      : "");
  const response = await fetch(`${baseUrl}/api/public/ai/chat/stream`, {
    method: "POST",
    credentials: "include",
    cache: "no-store",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream, application/json" },
    body: JSON.stringify({ sessionId, messages }),
  });
  const contentType = response.headers.get("content-type") ?? "";
  if (!response.ok || !contentType.includes("text/event-stream")) {
    const rawText = await response.text();
    try {
      const payload = JSON.parse(rawText) as { code?: string; message?: string };
      throw new ApiError(
        payload.code ?? "HTTP_ERROR",
        payload.message ?? `请求失败 (${response.status})`,
        response.status
      );
    } catch (error) {
      if (error instanceof ApiError) throw error;
      throw new ApiError("HTTP_ERROR", `请求失败 (${response.status})`, response.status);
    }
  }
  if (!response.body) {
    throw new ApiError("HTTP_ERROR", "AI 服务未返回内容", response.status);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split("\n\n");
    buffer = blocks.pop() ?? "";
    for (const block of blocks) {
      const parsed = parseSseBlock(block);
      if (parsed) onEvent(parsed.event, parsed.data);
    }
  }
  if (buffer.trim()) {
    const parsed = parseSseBlock(buffer);
    if (parsed) onEvent(parsed.event, parsed.data);
  }
}

function parseSseBlock(block: string): { event: "meta" | "delta" | "done" | "error"; data: Record<string, unknown> } | null {
  let event = "delta";
  const dataLines: string[] = [];
  for (const line of block.split("\n")) {
    if (line.startsWith("event:")) {
      event = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trimStart());
    }
  }
  if (event !== "meta" && event !== "delta" && event !== "done" && event !== "error") {
    return null;
  }
  if (dataLines.length === 0) {
    return { event, data: {} };
  }
  try {
    const data = JSON.parse(dataLines.join("\n")) as Record<string, unknown>;
    return { event, data };
  } catch {
    return { event, data: { text: dataLines.join("\n") } };
  }
}

export function uploadImage(
  file: File,
  options?: { onProgress?: (percent: number) => void }
) {
  const body = new FormData();
  body.append("file", file);
  return apiUpload<{ url: string; originalFilename: string }>(
    "/api/admin/media/images",
    body,
    options?.onProgress
  );
}

export function publishPost(postId: string) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}/publish`, {
    method: "POST",
  });
}

export function unpublishPost(postId: string) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}/unpublish`, {
    method: "POST",
  });
}

export function batchUnpublishPosts(ids: string[]) {
  return apiRequest<BatchPostActionResult>("/api/admin/posts/batch-unpublish", {
    method: "POST",
    body: { ids },
  });
}

export function batchTrashPosts(ids: string[]) {
  return apiRequest<BatchPostActionResult>("/api/admin/posts/batch-trash", {
    method: "POST",
    body: { ids },
  });
}

export function trashPost(postId: string) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}/trash`, {
    method: "POST",
  });
}

export function restorePost(postId: string) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}/restore`, {
    method: "POST",
  });
}

export function permanentlyDeletePost(postId: string) {
  return apiRequest<void>(`/api/admin/posts/${postId}`, {
    method: "DELETE",
  });
}

export function previewMarkdown(markdown: string) {
  return apiRequest<MarkdownPreview>("/api/admin/markdown/preview", {
    method: "POST",
    body: { markdown },
  });
}

export function importMarkdown(
  file: File,
  options?: {
    images?: File[];
    targetPostId?: string;
    confirmOverwrite?: boolean;
    onProgress?: (percent: number) => void;
  }
) {
  const body = new FormData();
  body.append("file", file);
  options?.images?.forEach((image) => body.append("images", image));
  if (options?.targetPostId) {
    body.append("targetPostId", options.targetPostId);
  }
  if (options?.confirmOverwrite) {
    body.append("confirmOverwrite", "true");
  }
  return apiUpload<ImportPostResult>("/api/admin/imports", body, options?.onProgress);
}

export function exportPost(postId: string, fallbackName = "post.md") {
  return downloadFile(`/api/admin/posts/${postId}/export`, fallbackName);
}

export function listCategories() {
  return apiRequest<Category[]>("/api/admin/categories");
}

export function createCategory(name: string, slug?: string, description?: string) {
  return apiRequest<Category>("/api/admin/categories", {
    method: "POST",
    body: { name, slug: slug || undefined, description: description || undefined },
  });
}

export function updateCategory(id: string, name: string, description?: string) {
  return apiRequest<Category>(`/api/admin/categories/${id}`, {
    method: "PUT",
    body: { name, description: description || undefined },
  });
}

export function updateCategorySlug(id: string, slug: string) {
  return apiRequest<Category>(`/api/admin/categories/${id}/slug`, {
    method: "PUT",
    body: slug,
  });
}

export function deleteCategory(id: string) {
  return apiRequest<void>(`/api/admin/categories/${id}`, { method: "DELETE" });
}

export function listTags() {
  return apiRequest<Tag[]>("/api/admin/tags");
}

export function createTag(name: string, slug?: string) {
  return apiRequest<Tag>("/api/admin/tags", {
    method: "POST",
    body: { name, slug: slug || undefined },
  });
}

export function updateTag(id: string, name: string) {
  return apiRequest<Tag>(`/api/admin/tags/${id}`, {
    method: "PUT",
    body: { name },
  });
}

export function updateTagSlug(id: string, slug: string) {
  return apiRequest<Tag>(`/api/admin/tags/${id}/slug`, {
    method: "PUT",
    body: slug,
  });
}

export function deleteTag(id: string) {
  return apiRequest<void>(`/api/admin/tags/${id}`, { method: "DELETE" });
}

export function getPublicSiteSettings() {
  return apiRequest<SiteSettings>("/api/public/site");
}

export function getAdminSiteSettings() {
  return apiRequest<SiteSettings>("/api/admin/site");
}

export function updateSiteSettings(payload: Omit<SiteSettings, "aboutHtml" | "updatedAt">) {
  return apiRequest<SiteSettings>("/api/admin/site", {
    method: "PUT",
    body: payload,
  });
}

export function listPublicComments(slug: string) {
  return apiRequest<PublicComment[]>(
    `/api/public/posts/${encodeURIComponent(slug)}/comments`
  );
}

export function createPublicComment(
  slug: string,
  authorName: string,
  content: string
) {
  return apiRequest<PublicComment>(
    `/api/public/posts/${encodeURIComponent(slug)}/comments`,
    { method: "POST", body: { authorName, content } }
  );
}

export function listAdminComments() {
  return apiRequest<AdminComment[]>("/api/admin/comments");
}

export function deleteAdminComment(commentId: string) {
  return apiRequest<void>(`/api/admin/comments/${commentId}`, { method: "DELETE" });
}

export function listPostRevisions(postId: string) {
  return apiRequest<PostRevisionSummary[]>(`/api/admin/posts/${postId}/revisions`);
}

export function getPostRevision(postId: string, revisionId: string) {
  return apiRequest<PostRevisionDetail>(
    `/api/admin/posts/${postId}/revisions/${revisionId}`
  );
}

export function restorePostRevision(postId: string, revisionId: string) {
  return apiRequest<AdminPost>(
    `/api/admin/posts/${postId}/revisions/${revisionId}/restore`,
    { method: "POST" }
  );
}

export function changePassword(currentPassword: string, newPassword: string) {
  return apiRequest<{ message: string }>("/api/auth/password", {
    method: "PUT",
    body: { currentPassword, newPassword },
  });
}

export function forgotPassword(email: string) {
  return apiRequest<{ message: string }>("/api/auth/forgot-password", {
    method: "POST",
    body: { email },
  });
}

export function resetPassword(token: string, newPassword: string) {
  return apiRequest<{ message: string }>("/api/auth/reset-password", {
    method: "POST",
    body: { token, newPassword },
  });
}
