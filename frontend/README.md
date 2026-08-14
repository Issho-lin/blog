# frontend

Next.js 公开站 + 管理端前端，对接 `backend` REST API。

## 技术栈

- Next.js 16（App Router）+ React 19 + TypeScript
- Tailwind CSS 4
- 通过 `next.config.ts` rewrite 把 `/api`、`/uploads` 代理到后端

## 已实现页面

| 路径 | 说明 |
| --- | --- |
| `/` | 公开首页（SSR 拉文章列表） |
| `/posts/[slug]` | 文章详情（HTML、目录、SEO meta） |
| `/archive` | 按年月归档 |
| `/categories`、`/categories/[slug]` | 分类列表与分类下文章 |
| `/tags`、`/tags/[slug]` | 标签列表与标签下文章 |
| `/search` | 公开搜索 |
| `/about` | 关于页（站点设置里的 Markdown） |
| `/admin/login` | 作者登录 |
| `/admin/posts` | 管理端文章列表、搜索、按状态筛选、回收站、导入导出 |
| `/admin/posts/[id]` | Markdown 编辑器：自动保存、预览、发布、导出 |
| `/admin/taxonomy` | 分类与标签增删改 |
| `/admin/settings` | 站点名称、简介、关于页等内容 |

## 编辑器说明

- 使用 **Vditor** 即时渲染（IR），体验接近语雀：工具栏排版、大纲、预览、可切换所见即所得。
- 支持 **Mermaid 文本绘图**（流程图 / 时序图 / 类图等）；编辑器可一键插入模板，公开文章页自动渲染。
- 标题 / 正文变更后 **1 秒防抖** 自动保存，携带 `expectedVersion` 做乐观锁。
- 图片上传走 `/api/admin/media/images`，粘贴或工具栏上传后插入 Markdown。
- 支持导入 `.md` 为草稿（解析 Front Matter），以及导出带 Front Matter 的 UTF-8 Markdown。
- 版本冲突（409 `CONCURRENT_MODIFICATION`）时提示重新加载，避免覆盖他人修改。
- 离开页面前若有未保存改动，浏览器会弹出确认。

## 启动

先确保后端在 `8080` 运行，然后：

```bash
cd frontend
pnpm install
pnpm dev
```

打开 [http://localhost:3000](http://localhost:3000)。

默认管理员（与后端一致）：

- 邮箱：`admin@blog.com`
- 密码：`admin123`

## 环境变量

见 `.env.local`：

```text
NEXT_PUBLIC_SITE_NAME=Linqibin Blog
NEXT_PUBLIC_API_BASE_URL=
API_PROXY_ORIGIN=http://localhost:8080
```

`NEXT_PUBLIC_API_BASE_URL` 留空时，浏览器请求走同源 `/api`（由 Next rewrite 转发）；服务端 SSR 直连 `API_PROXY_ORIGIN`。
