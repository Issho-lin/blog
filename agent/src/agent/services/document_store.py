from dataclasses import dataclass, field
from typing import Any


@dataclass
class StoredDocument:
    project_id: str
    doc_id: str
    corpus: str
    title: str
    text: str
    content_type: str
    metadata: dict[str, Any] = field(default_factory=dict)


class InMemoryDocumentStore:
    """进程内文档仓库，后续替换为 pgvector，不影响 API 形态。"""

    def __init__(self) -> None:
        self._documents: dict[tuple[str, str], StoredDocument] = {}

    def upsert(self, document: StoredDocument) -> StoredDocument:
        self._documents[(document.project_id, document.doc_id)] = document
        return document

    def get(self, project_id: str, doc_id: str) -> StoredDocument | None:
        return self._documents.get((project_id, doc_id))

    def delete(self, project_id: str, doc_id: str) -> bool:
        return self._documents.pop((project_id, doc_id), None) is not None

    def clear(self) -> None:
        self._documents.clear()
