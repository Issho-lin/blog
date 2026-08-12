import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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
