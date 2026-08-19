from pydantic import BaseModel, Field


class LlmOptions(BaseModel):
    base_url: str = ""
    api_key: str = ""
    model: str = Field(default="", max_length=200)


class EmbedOptions(BaseModel):
    base_url: str = ""
    api_key: str = ""
    model: str = Field(default="", max_length=200)
    dimensions: int | None = Field(default=None, ge=8, le=4096)
