from fastapi.testclient import TestClient


def test_upsert_get_and_delete_document(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    payload = {
        "corpus": "published",
        "title": "示例文章",
        "text": "这是正文。",
        "content_type": "markdown",
        "metadata": {"slug": "example", "url": "/posts/example"},
    }

    put_response = client.put(
        "/v1/projects/blog/documents/post-1",
        json=payload,
        headers=api_headers,
    )
    assert put_response.status_code == 200
    body = put_response.json()
    assert body["project_id"] == "blog"
    assert body["doc_id"] == "post-1"
    assert body["metadata"]["slug"] == "example"

    get_response = client.get("/v1/projects/blog/documents/post-1", headers=api_headers)
    assert get_response.status_code == 200
    assert get_response.json()["title"] == "示例文章"

    delete_response = client.delete(
        "/v1/projects/blog/documents/post-1",
        headers=api_headers,
    )
    assert delete_response.status_code == 204

    missing = client.get("/v1/projects/blog/documents/post-1", headers=api_headers)
    assert missing.status_code == 404


def test_delete_missing_document_is_idempotent(
    client: TestClient,
    api_headers: dict[str, str],
) -> None:
    response = client.delete("/v1/projects/blog/documents/missing", headers=api_headers)
    assert response.status_code == 204
