from langchain_core.language_models.chat_models import BaseChatModel
from langchain_openai import ChatOpenAI
from llama_index.core.base.embeddings.base import BaseEmbedding
from llama_index.core.embeddings.mock_embed_model import MockEmbedding
from llama_index.embeddings.openai import OpenAIEmbedding
from llama_index.embeddings.openai.base import OpenAIEmbeddingModelType

from agent.config import Settings
from agent.exceptions import LlmNotConfiguredError
from agent.schemas.llm import EmbedOptions, LlmOptions


def build_chat_model(settings: Settings) -> BaseChatModel | None:
    if not settings.llm_configured:
        return None
    return build_chat_model_from(
        settings.chat_base_url,
        settings.chat_api_key,
        settings.chat_model,
    )


def build_chat_model_from(base_url: str, api_key: str, model: str) -> BaseChatModel:
    resolved_base = base_url.rstrip("/") if base_url else None
    return ChatOpenAI(
        model=model,
        api_key=api_key,
        temperature=0.4,
        base_url=resolved_base,
    )


def resolve_chat_model(
    default: BaseChatModel | None,
    llm: LlmOptions | None,
    settings: Settings,
) -> BaseChatModel:
    if llm is not None and llm.api_key.strip() and llm.model.strip():
        return build_chat_model_from(llm.base_url, llm.api_key, llm.model)
    if default is not None:
        return default
    built = build_chat_model(settings)
    if built is None:
        raise LlmNotConfiguredError()
    return built


def build_embed_model(settings: Settings) -> BaseEmbedding:
    api_key = settings.resolved_embed_api_key
    if not api_key:
        return MockEmbedding(embed_dim=settings.embed_dimensions)
    return build_embed_model_from(
        settings.resolved_embed_base_url,
        api_key,
        settings.resolved_embed_model,
        settings.embed_dimensions,
    )


def build_embed_model_from(
    base_url: str,
    api_key: str,
    model: str,
    dimensions: int,
) -> BaseEmbedding:
    api_base = base_url.rstrip("/") if base_url else None
    allowed = {item.value for item in OpenAIEmbeddingModelType}
    init_model = model if model in allowed else "text-embedding-3-small"
    embedding = OpenAIEmbedding(
        model=init_model,
        api_key=api_key,
        api_base=api_base,
        dimensions=dimensions,
    )
    if model not in allowed:
        embedding.model_name = model
        embedding._query_engine = model
        embedding._text_engine = model
    return embedding


def resolve_embed_model(
    default: BaseEmbedding,
    embed: EmbedOptions | None,
    settings: Settings,
) -> tuple[BaseEmbedding, int]:
    dimensions = settings.embed_dimensions
    if embed is not None and embed.dimensions is not None:
        dimensions = embed.dimensions
    if embed is not None and embed.api_key.strip() and embed.model.strip():
        return (
            build_embed_model_from(embed.base_url, embed.api_key, embed.model, dimensions),
            dimensions,
        )
    return default, dimensions
