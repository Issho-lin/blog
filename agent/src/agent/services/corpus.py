from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any

from llama_index.core import Document, StorageContext, VectorStoreIndex
from llama_index.core.base.embeddings.base import BaseEmbedding
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core.schema import NodeWithScore
from llama_index.core.vector_stores.types import FilterOperator, MetadataFilter, MetadataFilters

from agent.config import Settings
from agent.schemas.llm import EmbedOptions
from agent.services.document_store import StoredDocument
from agent.services.models import resolve_embed_model


def _async_database_url(database_url: str) -> str:
    if database_url.startswith("postgresql+asyncpg://"):
        return database_url
    if database_url.startswith("postgresql+psycopg2://"):
        return "postgresql+asyncpg://" + database_url.removeprefix("postgresql+psycopg2://")
    if database_url.startswith("postgresql://"):
        return "postgresql+asyncpg://" + database_url.removeprefix("postgresql://")
    return database_url


def _safe_db_url(database_url: str) -> str:
    from urllib.parse import urlparse

    parsed = urlparse(database_url)
    return f"{parsed.scheme}://{parsed.hostname}:{parsed.port}{parsed.path}"


def ref_doc_id(project_id: str, doc_id: str) -> str:
    return f"{project_id}:{doc_id}"


@dataclass
class RetrievedChunk:
    doc_id: str
    title: str
    text: str
    score: float
    metadata: dict[str, Any]


class LlamaIndexCorpus:
    """用 LlamaIndex 做切片、embedding 与检索；可选 pgvector。"""

    def __init__(
        self,
        embed_model: BaseEmbedding,
        database_url: str = "",
        embed_dim: int = 1536,
    ) -> None:
        self._embed_model = embed_model
        self._database_url = database_url
        self._embed_dim = embed_dim
        self._splitter = SentenceSplitter(chunk_size=512, chunk_overlap=64)
        self._index = self._build_index(database_url, embed_dim)

    def apply_embed(self, embed: EmbedOptions | None, settings: Settings) -> None:
        model, dimensions = resolve_embed_model(self._embed_model, embed, settings)
        self.use_embed(model, dimensions)

    def use_embed(self, embed_model: BaseEmbedding, embed_dim: int) -> None:
        if embed_model is self._embed_model and embed_dim == self._embed_dim:
            return
        if embed_dim != self._embed_dim:
            self._embed_model = embed_model
            self._embed_dim = embed_dim
            self._index = self._build_index(self._database_url, embed_dim)
            return
        self._embed_model = embed_model
        self._index._embed_model = embed_model

    def _build_index(self, database_url: str, embed_dim: int) -> VectorStoreIndex:
        if not database_url:
            return VectorStoreIndex(nodes=[], embed_model=self._embed_model)

        from llama_index.core import VectorStoreIndex as PgIndex
        from llama_index.vector_stores.postgres import PGVectorStore

        async_url = _async_database_url(database_url)
        # #region agent log
        try:
            import json, time
            with open("/Users/linqibin/Documents/code/blog/.cursor/debug-b0a834.log", "a") as _f:
                _f.write(json.dumps({"sessionId": "b0a834", "runId": "post-fix", "hypothesisId": "F", "location": "corpus.py:_build_index", "message": "pgvector urls", "data": {"syncUrl": _safe_db_url(database_url), "asyncUrl": _safe_db_url(async_url), "embedDim": embed_dim}, "timestamp": int(time.time() * 1000)}) + "\n")
        except Exception:
            pass
        # #endregion
        vector_store = PGVectorStore.from_params(
            connection_string=database_url,
            async_connection_string=async_url,
            table_name=f"llamaindex_chunks_{embed_dim}",
            embed_dim=embed_dim,
            perform_setup=True,
            use_jsonb=True,
        )
        storage_context = StorageContext.from_defaults(vector_store=vector_store)
        return PgIndex.from_vector_store(
            vector_store,
            embed_model=self._embed_model,
            storage_context=storage_context,
        )

    def upsert(self, document: StoredDocument) -> None:
        key = ref_doc_id(document.project_id, document.doc_id)
        self.delete(document.project_id, document.doc_id)
        extra = {
            key: value
            for key, value in document.metadata.items()
            if key not in {"slug", "url"}
        }
        llama_doc = Document(
            text=f"{document.title}\n\n{document.text}",
            id_=key,
            metadata={
                "project_id": document.project_id,
                "doc_id": document.doc_id,
                "corpus": document.corpus,
                "title": document.title,
                "slug": str(document.metadata.get("slug", "")),
                "url": str(document.metadata.get("url", "")),
                "extra_json": json.dumps(extra, ensure_ascii=False),
            },
        )
        nodes = self._splitter.get_nodes_from_documents([llama_doc])
        self._index.insert_nodes(nodes)

    def delete(self, project_id: str, doc_id: str) -> None:
        self._index.delete_ref_doc(ref_doc_id(project_id, doc_id), delete_from_docstore=True)

    def retrieve(
        self,
        project_id: str,
        corpus: str,
        query: str,
        top_k: int,
        min_score: float,
    ) -> list[RetrievedChunk]:
        filters = MetadataFilters(
            filters=[
                MetadataFilter(key="project_id", value=project_id, operator=FilterOperator.EQ),
                MetadataFilter(key="corpus", value=corpus, operator=FilterOperator.EQ),
            ]
        )
        nodes = self._index.as_retriever(similarity_top_k=top_k, filters=filters).retrieve(query)
        chunks: list[RetrievedChunk] = []
        for node in nodes:
            score = float(node.score or 0)
            if score < min_score:
                continue
            chunks.append(self._to_chunk(node, score))
        return chunks

    def clear(self) -> None:
        # 仅内存索引可整体清空；pgvector 场景按文档删除。
        self._index = VectorStoreIndex(nodes=[], embed_model=self._embed_model)

    def _to_chunk(self, node: NodeWithScore, score: float) -> RetrievedChunk:
        metadata = dict(node.metadata or {})
        extra_raw = metadata.pop("extra_json", "")
        extra: dict[str, Any] = {}
        if extra_raw:
            try:
                parsed = json.loads(extra_raw)
                if isinstance(parsed, dict):
                    extra = parsed
            except json.JSONDecodeError:
                extra = {}
        restored = {
            **extra,
            "slug": metadata.get("slug", ""),
            "url": metadata.get("url", ""),
        }
        return RetrievedChunk(
            doc_id=str(metadata.get("doc_id", "")),
            title=str(metadata.get("title", "")),
            text=node.get_content(),
            score=score,
            metadata=restored,
        )
