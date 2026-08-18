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

    # 预留：独立向量库，不与业务库混用。
    database_url: str = ""

    chat_base_url: str = ""
    chat_api_key: str = ""
    chat_model: str = ""


@lru_cache
def get_settings() -> Settings:
    return Settings()
