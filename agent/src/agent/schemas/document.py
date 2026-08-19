from typing import Any

from pydantic import BaseModel, Field

from agent.schemas.llm import EmbedOptions


class DocumentUpsertRequest(BaseModel):
    corpus: str = Field(min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=500)
    text: str = Field(min_length=1)
    content_type: str = Field(default="markdown", max_length=64)
    metadata: dict[str, Any] = Field(default_factory=dict)
    embed: EmbedOptions | None = None


class DocumentResponse(BaseModel):
    project_id: str
    doc_id: str
    corpus: str
    title: str
    text: str
    content_type: str
    metadata: dict[str, Any]
