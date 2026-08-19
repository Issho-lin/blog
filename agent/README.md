# AI 平台服务

与博客业务解耦的 Python 服务：LlamaIndex 负责语料切片与检索，LangChain 负责摘要、帮写和对话。

要实现的能力、边界和分期见仓库文档：[docs/AI功能规划.md](../docs/AI功能规划.md)。

## 启动

```bash
cd agent
cp .env.example .env
# 在 .env 中填写 AGENT_CHAT_API_KEY / AGENT_CHAT_MODEL
uv sync
uv run agent
```

默认监听 `http://localhost:8090`。健康检查：`GET /health`（无需密钥）。

业务接口需要请求头 `X-API-Key`（默认 `dev-agent-key`）。

`pnpm dev` 会一并启动 Compose 服务 `ai-postgres`（宿主机 **5433**，库 `blog_ai`），并把 `AGENT_DATABASE_URL` 传给 agent。单独跑时：

```bash
docker compose up -d ai-postgres
# agent/.env:
# AGENT_DATABASE_URL=postgresql://blog:blog@localhost:5433/blog_ai
```

未配置 `AGENT_DATABASE_URL` 时，索引存在内存中，进程退出即丢失。对话模型、向量模型均可在博客后台「站点设置 → AI 模型」填写。请求体会带 `llm` / `embed` 覆盖环境变量；两边都没有对话密钥时 `/v1/complete` 与 `/v1/chat` 返回 503。未配置 embedding 时入库用 MockEmbedding，检索质量不可用，公开助手请同时填写向量模型。

```bash
curl -X PUT http://localhost:8090/v1/projects/blog/documents/post-1 \
  -H "X-API-Key: dev-agent-key" \
  -H "Content-Type: application/json" \
  -d '{"corpus":"published","title":"示例","text":"正文","metadata":{"url":"/posts/example"}}'
```

OpenAPI：`http://localhost:8090/docs`

## 测试

```bash
uv run pytest
```
