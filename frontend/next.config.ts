import path from "node:path";
import { fileURLToPath } from "node:url";
import type { NextConfig } from "next";

const workspaceRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");

const nextConfig: NextConfig = {
  // 仓库根同时有 pnpm-lock.yaml，显式指定 workspace，避免 Next 误推断。
  outputFileTracingRoot: workspaceRoot,
  turbopack: {
    root: workspaceRoot,
  },
  // 开发时把 /api 代理到后端，浏览器同源携带 Cookie 更省事。
  async rewrites() {
    const apiOrigin = process.env.API_PROXY_ORIGIN ?? "http://localhost:8080";
    return [
      {
        source: "/api/:path*",
        destination: `${apiOrigin}/api/:path*`,
      },
      {
        source: "/uploads/:path*",
        destination: `${apiOrigin}/uploads/:path*`,
      },
    ];
  },
};

export default nextConfig;
