from fastapi.testclient import TestClient
from llama_index.core.embeddings.mock_embed_model import MockEmbedding

from agent.config import Settings
from agent.main import create_app
from agent.services.container import build_services


def test_complete_summarize_returns_model_text(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    response = client.post(
        "/v1/complete",
        json={"scenario": "summarize", "text": "一段很长的正文，用来生成摘要。"},
        headers=api_headers,
    )
    assert response.status_code == 200
    assert response.json()["text"] == "模型输出"


def test_complete_without_llm_returns_unavailable(api_headers: dict[str, str]) -> None:
    services = build_services(
        settings=Settings(embed_dimensions=8),
        chat_model=None,
        embed_model=MockEmbedding(embed_dim=8),
    )
    with TestClient(create_app(services)) as client:
        response = client.post(
            "/v1/complete",
            json={"scenario": "summarize", "text": "正文"},
            headers=api_headers,
        )
        assert response.status_code == 503


def test_chat_without_llm_returns_unavailable(api_headers: dict[str, str]) -> None:
    services = build_services(
        settings=Settings(embed_dimensions=8),
        chat_model=None,
        embed_model=MockEmbedding(embed_dim=8),
    )
    with TestClient(create_app(services)) as client:
        response = client.post(
            "/v1/chat",
            json={"messages": [{"role": "user", "content": "你好"}]},
            headers=api_headers,
        )
        assert response.status_code == 503


def test_chat_with_rag_returns_citations(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    upsert = client.put(
        "/v1/projects/blog/documents/post-1",
        json={
            "corpus": "published",
            "title": "向量检索入门",
            "text": "向量检索把文本变成向量再找相似段落。",
            "metadata": {"slug": "vector-search", "url": "/posts/vector-search"},
        },
        headers=api_headers,
    )
    assert upsert.status_code == 200

    response = client.post(
        "/v1/chat",
        json={
            "project_id": "blog",
            "messages": [{"role": "user", "content": "向量检索把文本变成向量再找相似段落。"}],
            "rag": {"enabled": True, "corpus": "published", "top_k": 3},
        },
        headers=api_headers,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["text"] == "模型输出"
    matched = next(item for item in body["citations"] if item["doc_id"] == "post-1")
    assert matched["metadata"]["url"] == "/posts/vector-search"


def test_chat_stream_emits_sse_events(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    response = client.post(
        "/v1/chat",
        json={"messages": [{"role": "user", "content": "你好"}], "stream": True},
        headers=api_headers,
    )
    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")
    body = response.text
    assert "event: meta" in body
    assert "event: delta" in body
    assert "event: done" in body
    assert all(piece in body for piece in "模型输出")


def test_upsert_accepts_embed_options(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    response = client.put(
        "/v1/projects/blog/documents/post-embed",
        json={
            "corpus": "published",
            "title": "嵌入配置",
            "text": "入库时可以带上向量模型配置。",
            "embed": {"base_url": "", "api_key": "", "model": "", "dimensions": 8},
        },
        headers=api_headers,
    )
    assert response.status_code == 200
