from typing import Any, Literal

from pydantic import BaseModel, Field


class CompleteRequest(BaseModel):
    scenario: Literal["summarize", "write", "chat"]
    instruction: str = ""
    text: str = ""
    context: str = ""
    mode: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)


class CompleteResponse(BaseModel):
    text: str
    finish_reason: str = "stop"
