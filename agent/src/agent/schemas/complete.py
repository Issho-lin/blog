from typing import Any, Literal

from pydantic import BaseModel, Field

from agent.schemas.llm import LlmOptions


class CompleteRequest(BaseModel):
    scenario: Literal["summarize", "write", "chat", "taxonomy"]
    instruction: str = ""
    text: str = ""
    context: str = ""
    mode: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)
    llm: LlmOptions | None = None


class CompleteResponse(BaseModel):
    text: str
    finish_reason: str = "stop"
