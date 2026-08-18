from collections.abc import Generator

import pytest
from fastapi.testclient import TestClient

from agent.api.v1 import documents
from agent.config import get_settings
from agent.main import create_app


@pytest.fixture
def client() -> Generator[TestClient]:
    get_settings.cache_clear()
    documents.store.clear()
    with TestClient(create_app()) as test_client:
        yield test_client
    get_settings.cache_clear()


@pytest.fixture
def api_headers() -> dict[str, str]:
    return {"X-API-Key": get_settings().api_key}
