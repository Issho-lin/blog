from fastapi.testclient import TestClient


def test_health_does_not_require_api_key(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
