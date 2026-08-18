# AI 平台服务

与博客业务解耦的 Python 服务：接收通用文档、提供补全/对话接口。当前为基础框架，文档暂存内存，LLM 与向量库尚未接入。

要实现的能力、边界和分期见仓库文档：[docs/AI功能规划.md](../docs/AI功能规划.md)。

## 启动

```bash
cd agent
cp .env.example .env
uv sync
uv run agent
```

默认监听 `http://localhost:8090`。健康检查：`GET /health`（无需密钥）。

业务接口需要请求头 `X-API-Key`（默认 `dev-agent-key`）。

```bash
# 写入一篇文档
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
