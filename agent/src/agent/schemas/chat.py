from typing import Any, Literal

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str = Field(min_length=1)


class RagOptions(BaseModel):
    enabled: bool = False
    corpus: str = "published"
    top_k: int = Field(default=6, ge=1, le=20)


class ChatRequest(BaseModel):
    session_id: str | None = None
    messages: list[ChatMessage] = Field(min_length=1)
    rag: RagOptions = Field(default_factory=RagOptions)
    metadata: dict[str, Any] = Field(default_factory=dict)


class Citation(BaseModel):
    doc_id: str
    title: str
    metadata: dict[str, Any] = Field(default_factory=dict)


class ChatResponse(BaseModel):
    session_id: str | None
    text: str
    citations: list[Citation] = Field(default_factory=list)
