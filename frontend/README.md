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
| `/admin/login` | 作者登录 |
| `/admin/posts` | 管理端文章列表、新建草稿、发布 / 下线 |
| `/admin/posts/[id]` | Markdown 编辑器：自动保存、预览、发布 |

## 编辑器说明

- 使用 **Vditor** 即时渲染（IR），体验接近语雀：工具栏排版、大纲、预览、可切换所见即所得。
- 标题 / 正文变更后 **1 秒防抖** 自动保存，携带 `expectedVersion` 做乐观锁。
- 图片上传走 `/api/admin/media/images`，粘贴或工具栏上传后插入 Markdown。
- 版本冲突（409 `CONCURRENT_MODIFICATION`）时提示重新加载，避免覆盖他人修改。
- 离开页面前若有未保存改动，浏览器会弹出确认。

## 启动

先确保后端在 `8080` 运行，然后：

```bash
cd frontend
npm install
npm run dev
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

## 下一步

- 归档 / 分类 / 标签 / 搜索公开页
- 导入导出与图片上传 UI
- 分类标签管理页
