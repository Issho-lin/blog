import { apiRequest } from "./client";
import type {
  AdminPost,
  AuthUser,
  Category,
  MarkdownPreview,
  PageResponse,
  PublicPostDetail,
  PublicPostSummary,
  Tag,
  UpdatePostInput,
} from "./types";

export function listPublishedPosts(page = 1, pageSize = 10) {
  return apiRequest<PageResponse<PublicPostSummary>>(
    `/api/public/posts?page=${page}&pageSize=${pageSize}`
  );
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

export function listAdminPosts(keyword?: string) {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  return apiRequest<AdminPost[]>(`/api/admin/posts${query}`);
}

export function getAdminPost(postId: string) {
  return apiRequest<AdminPost>(`/api/admin/posts/${postId}`);
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

export function previewMarkdown(markdown: string) {
  return apiRequest<MarkdownPreview>("/api/admin/markdown/preview", {
    method: "POST",
    body: { markdown },
  });
}

export function listCategories() {
  return apiRequest<Category[]>("/api/admin/categories");
}

export function listTags() {
  return apiRequest<Tag[]>("/api/admin/tags");
}
