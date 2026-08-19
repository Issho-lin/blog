from langchain_core.language_models.chat_models import BaseChatModel
from langchain_openai import ChatOpenAI
from llama_index.core.base.embeddings.base import BaseEmbedding
from llama_index.core.embeddings.mock_embed_model import MockEmbedding
from llama_index.embeddings.openai import OpenAIEmbedding

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
    # #region agent log
    try:
        import json, time
        from llama_index.embeddings.openai.base import OpenAIEmbeddingModelType
        allowed = [item.value for item in OpenAIEmbeddingModelType]
        with open("/Users/linqibin/Documents/code/blog/.cursor/debug-b0a834.log", "a") as _f:
            _f.write(json.dumps({"sessionId": "b0a834", "runId": "pre-fix", "hypothesisId": "A", "location": "models.py:build_embed_model_from", "message": "construct OpenAIEmbedding", "data": {"model": model, "apiBase": api_base, "dimensions": dimensions, "modelInOpenAiEnum": model in allowed, "allowedSample": allowed[:8]}, "timestamp": int(time.time() * 1000)}) + "\n")
    except Exception:
        pass
    # #endregion
    from llama_index.embeddings.openai.base import OpenAIEmbeddingModelType

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
    # #region agent log
    try:
        import json, time
        with open("/Users/linqibin/Documents/code/blog/.cursor/debug-b0a834.log", "a") as _f:
            _f.write(json.dumps({"sessionId": "b0a834", "runId": "post-fix", "hypothesisId": "A", "location": "models.py:build_embed_model_from:after", "message": "OpenAIEmbedding constructed", "data": {"requestedModel": model, "initModel": init_model, "engine": getattr(embedding, "_text_engine", None), "customModel": model not in allowed}, "timestamp": int(time.time() * 1000)}) + "\n")
    except Exception:
        pass
    # #endregion
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
        # #region agent log
        try:
            import json, time
            with open("/Users/linqibin/Documents/code/blog/.cursor/debug-b0a834.log", "a") as _f:
                _f.write(json.dumps({"sessionId": "b0a834", "runId": "pre-fix", "hypothesisId": "B", "location": "models.py:resolve_embed_model", "message": "using request embed override", "data": {"model": embed.model, "hasKey": True, "dimensions": dimensions, "defaultType": type(default).__name__}, "timestamp": int(time.time() * 1000)}) + "\n")
        except Exception:
            pass
        # #endregion
        return (
            build_embed_model_from(embed.base_url, embed.api_key, embed.model, dimensions),
            dimensions,
        )
    # #region agent log
    try:
        import json, time
        with open("/Users/linqibin/Documents/code/blog/.cursor/debug-b0a834.log", "a") as _f:
            _f.write(json.dumps({"sessionId": "b0a834", "runId": "pre-fix", "hypothesisId": "C", "location": "models.py:resolve_embed_model", "message": "fallback default embed", "data": {"hasEmbed": embed is not None, "embedModel": None if embed is None else embed.model, "defaultType": type(default).__name__, "dimensions": dimensions}, "timestamp": int(time.time() * 1000)}) + "\n")
    except Exception:
        pass
    # #endregion
    return default, dimensions
