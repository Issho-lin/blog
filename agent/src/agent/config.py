from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="AGENT_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    api_key: str = "dev-agent-key"
    host: str = "0.0.0.0"
    port: int = 8090

    # 独立向量库。为空则用内存索引（测试 / 未起 pgvector 时）。
    database_url: str = ""

    chat_base_url: str = ""
    chat_api_key: str = ""
    chat_model: str = ""

    embed_base_url: str = ""
    embed_api_key: str = ""
    embed_model: str = ""
    embed_dimensions: int = 1536

    rag_min_score: float = 0.25

    @property
    def llm_configured(self) -> bool:
        return bool(self.chat_api_key and self.chat_model)

    @property
    def resolved_embed_api_key(self) -> str:
        return self.embed_api_key or self.chat_api_key

    @property
    def resolved_embed_base_url(self) -> str:
        return self.embed_base_url or self.chat_base_url

    @property
    def resolved_embed_model(self) -> str:
        return self.embed_model or "text-embedding-3-small"


@lru_cache
def get_settings() -> Settings:
    return Settings()
