export type ApiResponse<T> = {
  code: string;
  message: string;
  data: T;
  requestId?: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
};

export type PublicPostSummary = {
  id: string;
  title: string;
  slug: string;
  summary: string | null;
  publishedAt: string | null;
  readingTimeMinutes: number;
  viewCount: number;
  categoryName: string | null;
  tagNames: string[];
};

export type TocItem = {
  level: number;
  text: string;
  anchor: string;
};

export type PublicPostDetail = {
  id: string;
  title: string;
  slug: string;
  html: string;
  markdownContent: string;
  summary: string | null;
  tableOfContents: TocItem[];
  readingTimeMinutes: number;
  viewCount: number;
  publishedAt: string | null;
  updatedAt: string | null;
  categoryName: string | null;
  tagNames: string[];
  seoTitle: string | null;
  seoDescription: string | null;
  canonicalUrl: string | null;
};

export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
  role: string;
  lastLoginAt: string | null;
};

export type AdminPost = {
  id: string;
  title: string;
  slug: string;
  markdownContent: string;
  status: "DRAFT" | "PUBLISHED" | "UNPUBLISHED" | "TRASHED";
  categoryId: string | null;
  tagIds: string[] | null;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
  version: number;
};

export type Category = {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Tag = {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
  updatedAt: string;
};

export type MarkdownPreview = {
  html: string;
  tableOfContents: TocItem[];
};

export type UpdatePostInput = {
  title: string;
  markdownContent: string;
  slug?: string | null;
  categoryId?: string | null;
  tagIds?: string[] | null;
  expectedVersion?: number | null;
};
