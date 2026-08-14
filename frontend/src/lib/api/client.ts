import type { ApiResponse } from "./types";

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

function resolveBaseUrl(): string {
  // 浏览器端优先用同源 /api；服务端 SSR 也可走 rewrite 相对路径。
  const configured = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();
  if (configured) {
    return configured.replace(/\/$/, "");
  }
  if (typeof window === "undefined") {
    return process.env.API_PROXY_ORIGIN?.replace(/\/$/, "") ?? "http://localhost:8080";
  }
  return "";
}

type RequestOptions = {
  method?: string;
  body?: unknown;
  headers?: HeadersInit;
  cache?: RequestCache;
  credentials?: RequestCredentials;
};

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const baseUrl = resolveBaseUrl();
  const url = `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;

  const headers = new Headers(options.headers);
  const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;
  if (options.body !== undefined && !isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    method: options.method ?? "GET",
    headers,
    body:
      options.body === undefined
        ? undefined
        : isFormData
          ? (options.body as FormData)
          : JSON.stringify(options.body),
    cache: options.cache ?? "no-store",
    credentials: options.credentials ?? "include",
  });

  const rawText = await response.text();
  if (!rawText) {
    if (!response.ok) {
      throw new ApiError("HTTP_ERROR", `请求失败 (${response.status})`, response.status);
    }
    return undefined as T;
  }

  const payload = JSON.parse(rawText) as ApiResponse<T>;

  if (!response.ok || payload.code !== "OK") {
    throw new ApiError(
      payload.code ?? "HTTP_ERROR",
      payload.message ?? `请求失败 (${response.status})`,
      response.status
    );
  }

  return payload.data;
}

function resolveUrl(path: string) {
  const baseUrl = resolveBaseUrl();
  return `${baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
}

function filenameFromDisposition(header: string | null, fallback: string) {
  if (!header) return fallback;
  const utf8 = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1]);
    } catch {
      return utf8[1];
    }
  }
  const ascii = header.match(/filename="([^"]+)"/i) ?? header.match(/filename=([^;]+)/i);
  return ascii?.[1]?.trim() || fallback;
}

/** 下载后端原始文件（如 Markdown 导出），不走统一 JSON 包装解析。 */
export async function downloadFile(path: string, fallbackName: string) {
  const response = await fetch(resolveUrl(path), {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) {
    const rawText = await response.text();
    try {
      const payload = JSON.parse(rawText) as ApiResponse<unknown>;
      throw new ApiError(
        payload.code ?? "HTTP_ERROR",
        payload.message ?? `下载失败 (${response.status})`,
        response.status
      );
    } catch (error) {
      if (error instanceof ApiError) throw error;
      throw new ApiError("HTTP_ERROR", `下载失败 (${response.status})`, response.status);
    }
  }

  const blob = await response.blob();
  const filename = filenameFromDisposition(
    response.headers.get("Content-Disposition"),
    fallbackName
  );
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}
