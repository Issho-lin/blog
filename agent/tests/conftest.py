from collections.abc import Generator

import pytest
from fastapi.testclient import TestClient
from langchain_core.language_models.fake_chat_models import FakeListChatModel
from llama_index.core.embeddings.mock_embed_model import MockEmbedding

from agent.config import Settings, get_settings
from agent.main import create_app
from agent.services.container import build_services


@pytest.fixture
def settings() -> Settings:
    get_settings.cache_clear()
    return Settings(embed_dimensions=8, rag_min_score=0.0)


@pytest.fixture
def fake_llm() -> FakeListChatModel:
    return FakeListChatModel(responses=["模型输出"])


@pytest.fixture
def client(settings: Settings, fake_llm: FakeListChatModel) -> Generator[TestClient]:
    services = build_services(
        settings=settings,
        chat_model=fake_llm,
        embed_model=MockEmbedding(embed_dim=8),
    )
    with TestClient(create_app(services)) as test_client:
        yield test_client
    get_settings.cache_clear()


@pytest.fixture
def api_headers() -> dict[str, str]:
    return {"X-API-Key": "dev-agent-key"}
