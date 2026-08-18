from fastapi.testclient import TestClient


def test_v1_rejects_missing_api_key(client: TestClient) -> None:
    response = client.get("/v1/projects/blog/documents/post-1")
    assert response.status_code == 401


def test_v1_rejects_wrong_api_key(client: TestClient) -> None:
    response = client.get(
        "/v1/projects/blog/documents/post-1",
        headers={"X-API-Key": "wrong"},
    )
    assert response.status_code == 401
