from dataclasses import dataclass
from typing import Any

from langchain_core.language_models.chat_models import BaseChatModel
from llama_index.core.base.embeddings.base import BaseEmbedding

from agent.config import Settings, get_settings
from agent.services.chat import ChatService
from agent.services.complete import CompleteService
from agent.services.corpus import LlamaIndexCorpus
from agent.services.document_store import InMemoryDocumentStore
from agent.services.models import build_chat_model, build_embed_model

_UNSET = object()


@dataclass
class AppServices:
    settings: Settings
    documents: InMemoryDocumentStore
    corpus: LlamaIndexCorpus
    complete: CompleteService
    chat: ChatService


def build_services(
    settings: Settings | None = None,
    chat_model: Any = _UNSET,
    embed_model: BaseEmbedding | None = None,
) -> AppServices:
    resolved = settings or get_settings()
    llm: BaseChatModel | None
    if chat_model is _UNSET:
        llm = build_chat_model(resolved)
    else:
        llm = chat_model
    embed = embed_model or build_embed_model(resolved)
    documents = InMemoryDocumentStore()
    corpus = LlamaIndexCorpus(
        embed_model=embed,
        database_url=resolved.database_url,
        embed_dim=resolved.embed_dimensions,
    )
    return AppServices(
        settings=resolved,
        documents=documents,
        corpus=corpus,
        complete=CompleteService(llm, resolved),
        chat=ChatService(llm, corpus, resolved),
    )
