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
  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
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
