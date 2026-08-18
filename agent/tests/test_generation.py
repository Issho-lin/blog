from fastapi.testclient import TestClient


def test_complete_validates_body_and_returns_not_implemented(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    response = client.post(
        "/v1/complete",
        json={"scenario": "summarize", "text": "一段很长的正文"},
        headers=api_headers,
    )
    assert response.status_code == 501


def test_chat_validates_body_and_returns_not_implemented(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    response = client.post(
        "/v1/chat",
        json={
            "messages": [{"role": "user", "content": "你好"}],
            "rag": {"enabled": True, "corpus": "published"},
        },
        headers=api_headers,
    )
    assert response.status_code == 501
