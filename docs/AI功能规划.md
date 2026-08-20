# AI 功能规划

博客要接三类能力：**AI 帮我写**、**AI 摘要**、**全局助手**（闲聊 + 按已发布文章检索回答）。

AI 本身做成独立 Python 服务（`agent/`），以后可以接到别的项目。博客 Java 后端只负责登录、限流，以及「什么时候把已发布文章寄给 AI」。

对照本文勾选进度。骨架已在 `agent/`，LLM、向量库、Java 同步、前端入口都还没做。

---

## 原则

- 浏览器只打 Java，不直连 AI 服务、不持有模型密钥。
- Python 不认识「文章 / 发布 / Session」。它只认识项目、文档、补全、对话。
- LLM **只建议、不自动落库**。帮写要用户点「插入」，摘要要用户点「采用」后再保存。
- 知识库 **只含已发布** 内容。草稿、下线、回收站不进检索。
- 博客库存原文；向量存在 AI 自己的库。

---

## 产品功能

### 1. AI 帮我写（管理端编辑器）

入口：文章编辑页工具栏。

| 模式 | 做什么 |
|---|---|
| 从零写 | 按题目或指令写一篇完整正文 |
| 续写 | 按当前正文往后写 |
| 润色 | 改写当前正文 |
| 按大纲写 | 标题 + 用户指令 → Markdown |
| 标题建议 | 根据正文给几个标题，不自动改字段 |

结果先流式显示在侧栏，用户确认后才写入编辑器。不自动保存，避免误冲版本。

**不走知识库。** 把编辑器里正在写的内容当场传给补全接口即可。

### 2. AI 摘要（管理端）

入口：摘要输入框旁「生成摘要」。

- 根据当前正文生成约 80～160 字，填入摘要框（最长 500，与现有字段一致）。
- 用户保存后才入库。
- 模型失败时，回退到现在的正文截取逻辑。

**不走知识库。**

### 2.1 AI 分类打标（管理端）

入口：文章设置里「AI 分类打标」。

- 根据标题和正文建议 **一个分类** 和 **1～5 个标签**。
- 优先用已有同名分类/标签；没有则立刻创建，再写回编辑器当前文章（仍需用户保存文章）。

**不走知识库。**

### 3. 全局助手（公开站 + 管理端均可）

悬浮对话窗口。

- **闲聊**：问候、自我介绍等，不检索。
- **问站内文章**：在已发布语料里检索，再生成回答，并带上来源（标题 + 链接）。
- 检索分数过低时，明确说站内没找到，不要编造「博客里写过」。
- 公开端匿名可用，但必须按 IP 限流。
- 助手不能改文章、不能执行管理操作。

管理端使用同一套助手时，可以额外带上「当前草稿」当上下文，但草稿仍不写入知识库。

---

## 语料怎么进 AI

真相源仍是博客里的文章。Java 在「访客能不能看到」变化时，把通用文档推给 Python。

| 博客操作 | 告诉 AI |
|---|---|
| 发布；或已发布后又改了标题/正文/slug | 写入或覆盖这篇文档 |
| 下线、进回收站 | 删除这篇文档 |
| 写草稿、自动保存、阅读数 +1 | 不推 |
| 从回收站恢复 | 不推（恢复后仍是草稿或已下线） |

文档编号用文章 id。正文带标题和 Markdown；链接、slug 放在 `metadata` 里，Python 原样还给 Java，用来展示引用。

已经发布过、但当时还没有 AI 的文章：做一次「把当前所有已发布文章再寄一遍」。

发布成功不依赖 AI：先保存文章，再异步推送；AI 挂了，博客仍能用。

---

## 向量存在哪里

存在 **AI 服务自己的 PostgreSQL**（`pgvector`），不要写入博客的 `posts` 表。

- 开发可用独立库（例如 `blog_ai`），与现在的 `blog` 库分开。
- Python 把文档切段、embedding 后写入该库。
- 同一 `doc_id` 覆盖时，旧切片整篇替换；删除则按 `doc_id` 清掉。

个人博客体量不必上独立向量云服务。

---

## 谁做什么

| Java 博客后端 | Python `agent/` |
|---|---|
| Session 登录、公开限流 | `X-API-Key` 服务间鉴权 |
| 发布/下线时同步文档 | 收文档、切片、存向量 |
| 把 `Post` 翻成通用 Document | 补全、对话、RAG |
| 后台保存模型 URL/Key/名称，随请求传给 agent | 请求体 `llm` 可覆盖默认模型 |
| 把引用链接交给前端 | 不拼接博客 URL |
| SSE 转给浏览器 | `POST /v1/chat` 可选 `stream` |

博客侧 `projectId = blog`，`corpus = published`。别的项目换自己的 projectId 即可。

---

## 接口约定（骨架已有）

服务默认 `http://localhost:8090`。除 `/health` 外需要 `X-API-Key`。

```text
GET    /health
PUT    /v1/projects/{projectId}/documents/{docId}
GET    /v1/projects/{projectId}/documents/{docId}
DELETE /v1/projects/{projectId}/documents/{docId}
POST   /v1/complete          # summarize | write | chat
POST   /v1/chat              # stream=true 时返回 SSE（meta / delta / done）

# 博客 Java 转发（需登录）
GET    /api/admin/ai/settings
PUT    /api/admin/ai/settings   # 对话模型 / 密钥；保存后立刻生效，密钥不回显
POST   /api/admin/ai/summarize
POST   /api/admin/ai/write
POST   /api/admin/ai/taxonomy
POST   /api/admin/ai/index/rebuild

# 公开站
GET    /api/public/ai/status
POST   /api/public/ai/chat         # Java 按 IP 限流后再转 agent（整段返回）
POST   /api/public/ai/chat/stream  # 同上，SSE 转发给浏览器
```

当前：文档会写入 LlamaIndex（默认为内存向量；可接 pgvector）。`complete` / `chat` 已接 LangChain。对话模型优先用后台配置，未填时回退 agent 环境变量；都没有则返回 503。

---

## 分期

### 已完成

- [x] `agent/` uv 项目与 FastAPI 骨架
- [x] 健康检查、API Key
- [x] 文档 PUT/GET/DELETE 契约（内存实现）
- [x] 补全 / 对话请求体形状

### 第一期：能生成

- [x] 接入 OpenAI 兼容的 Chat API（密钥只放 AI 服务）
- [x] `POST /v1/complete`：摘要、帮写（先非流式，再 SSE）
- [x] Java 管理接口转发（登录后才能调）
- [x] 编辑器：「生成摘要」「AI 帮我写」「AI 分类打标」，帮写结果需用户确认

### 第二期：语料与助手

- [x] AI 侧 PostgreSQL + pgvector（Compose 服务 `ai-postgres`，`pnpm dev` 一并启动）
- [x] 文档入库时切片并写入向量（LlamaIndex；无数据库时用内存）
- [x] Java：发布/下线异步同步文档；历史已发布文章回填（`POST /api/admin/ai/index/rebuild`）
- [x] `POST /v1/chat`：闲聊；开启 RAG 时检索已发布语料并带 citations
- [x] 公开站全局助手 UI + 限流 + SSE 流式输出
- [x] 管理后台动态配置对话模型与向量模型（密钥不回显）

### 第三期（可后置）

- [ ] 关键词 + 向量混合检索
- [ ] 对话历史持久化
- [x] 站点设置里配置助手人设
- [ ] Agent 多步工具（仍保持与博客表解耦）
- [ ] `agent/` 独立部署文档 / 镜像，方便拷到其他仓库

---

## 明确不做（本阶段）

- Python 直连博客 `posts` 表
- 前端直连模型和 `agent/`
- 草稿进入 RAG
- 生成结果未经确认就写入数据库
- 一上来上 LangGraph / 独立向量云
