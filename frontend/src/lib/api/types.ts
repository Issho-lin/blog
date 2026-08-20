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
  coverUrl: string | null;
  publishedAt: string | null;
  readingTimeMinutes: number;
  viewCount: number;
  categoryName: string | null;
  categorySlug: string | null;
  tagNames: string[];
  tagSlugs: string[];
};

export type TocItem = {
  level: number;
  text: string;
  anchor: string;
};

export type AdminPostPreview = PublicPostDetail & {
  status: AdminPost["status"];
};

export type PublicPostDetail = {
  id: string;
  title: string;
  slug: string;
  html: string;
  markdownContent: string;
  summary: string | null;
  coverUrl: string | null;
  tableOfContents: TocItem[];
  readingTimeMinutes: number;
  viewCount: number;
  publishedAt: string | null;
  updatedAt: string | null;
  categoryName: string | null;
  categorySlug: string | null;
  tagNames: string[];
  tagSlugs: string[];
  seoTitle: string | null;
  seoDescription: string | null;
  canonicalUrl: string | null;
  previousPost: PublicPostNeighbor | null;
  nextPost: PublicPostNeighbor | null;
};

export type PublicPostNeighbor = {
  title: string;
  slug: string;
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
  excerpt: string | null;
  coverUrl: string | null;
  seoTitle: string | null;
  seoDescription: string | null;
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

export type SiteSettings = {
  siteName: string;
  siteSubtitle: string;
  siteDescription: string;
  authorName: string;
  authorAvatarUrl: string;
  aboutMarkdown: string;
  aboutHtml: string;
  postsPerPage: number;
  timezone: string;
  defaultLanguage: string;
  faviconUrl: string;
  defaultShareImageUrl: string;
  updatedAt: string | null;
};

export type ArchiveItem = {
  title: string;
  slug: string;
  publishedAt: string | null;
};

export type ArchiveGroup = {
  year: number;
  month: number;
  items: ArchiveItem[];
};

export type UpdatePostInput = {
  title: string;
  markdownContent: string;
  slug?: string | null;
  categoryId?: string | null;
  tagIds?: string[] | null;
  expectedVersion?: number | null;
  excerpt?: string | null;
  coverUrl?: string | null;
  seoTitle?: string | null;
  seoDescription?: string | null;
};

export type AiTextResult = {
  text: string;
};

export type AiSettings = {
  enabled: boolean;
  assistantEnabled: boolean;
  chatBaseUrl: string;
  chatApiKeyConfigured: boolean;
  chatModel: string;
  embedBaseUrl: string;
  embedApiKeyConfigured: boolean;
  embedModel: string;
  embedDimensions: number;
  assistantPersona: string;
  ratePerMinute: number;
  ratePerDay: number;
  updatedAt: string | null;
};

export type PublicAiStatus = {
  assistantEnabled: boolean;
};

export type PublicAiCitation = {
  title: string;
  url: string;
};

export type PublicAiChatResult = {
  sessionId: string;
  text: string;
  citations: PublicAiCitation[];
};

export type BatchPostActionResult = {
  succeeded: AdminPost[];
  failed: { id: string; message: string }[];
};

export type AdminDashboardPost = {
  id: string;
  title: string;
  slug: string;
  status: AdminPost["status"];
  updatedAt: string;
  publishedAt: string | null;
  viewCount: number;
};

export type AdminDashboard = {
  counts: {
    total: number;
    published: number;
    draft: number;
    unpublished: number;
    trashed: number;
    publishedViewCount: number;
  };
  recentlyEdited: AdminDashboardPost[];
  recentlyPublished: AdminDashboardPost[];
};

export type ImportPostResult = {
  id: string;
  title: string;
  slug: string;
  status: string;
  warnings?: string[];
};

export type PublicComment = {
  id: string;
  postId: string;
  authorName: string;
  content: string;
  createdAt: string;
};

export type AdminComment = PublicComment & {
  postTitle: string;
  postSlug: string;
};

export type PostRevisionSummary = {
  id: string;
  title: string;
  kind: "AUTO" | "PUBLISH" | "RESTORE";
  createdAt: string;
};

export type PostRevisionDetail = PostRevisionSummary & {
  postId: string;
  markdownContent: string;
  excerpt: string | null;
};

