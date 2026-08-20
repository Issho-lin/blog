import json
from collections.abc import Iterator
from dataclasses import dataclass
from typing import Any

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from agent.config import Settings
from agent.schemas.chat import ChatMessage, ChatRequest, ChatResponse, Citation
from agent.services.corpus import LlamaIndexCorpus
from agent.services.models import resolve_chat_model


@dataclass
class _PreparedChat:
    model: BaseChatModel
    messages: list[SystemMessage | HumanMessage | AIMessage]
    citations: list[Citation]
    session_id: str | None


class ChatService:
    """LangChain 对话；需要时用 LlamaIndex 检索语料。"""

    def __init__(
        self,
        chat_model: BaseChatModel | None,
        corpus: LlamaIndexCorpus,
        settings: Settings,
    ) -> None:
        self._chat_model = chat_model
        self._corpus = corpus
        self._settings = settings

    def chat(self, request: ChatRequest) -> ChatResponse:
        prepared = self._prepare(request)
        result = prepared.model.invoke(prepared.messages)
        text = _chunk_text(result)
        return ChatResponse(
            session_id=prepared.session_id,
            text=text.strip(),
            citations=prepared.citations,
        )

    def stream(self, request: ChatRequest) -> Iterator[str]:
        try:
            prepared = self._prepare(request)
            yield _sse(
                "meta",
                {
                    "session_id": prepared.session_id,
                    "citations": [item.model_dump() for item in prepared.citations],
                },
            )
            for chunk in prepared.model.stream(prepared.messages):
                text = _chunk_text(chunk)
                if text:
                    yield _sse("delta", {"text": text})
            yield _sse("done", {})
        except Exception as error:
            yield _sse("error", {"message": str(error)})

    def _prepare(self, request: ChatRequest) -> _PreparedChat:
        model = resolve_chat_model(self._chat_model, request.llm, self._settings)

        question = _last_user_text(request.messages)
        citations: list[Citation] = []
        retrieved_block = ""
        if request.rag.enabled and question:
            self._corpus.apply_embed(request.embed, self._settings)
            chunks = self._corpus.retrieve(
                project_id=request.project_id,
                corpus=request.rag.corpus,
                query=question,
                top_k=request.rag.top_k,
                min_score=self._settings.rag_min_score,
            )
            if chunks:
                retrieved_block = "\n\n".join(
                    f"[{item.title}]({item.metadata.get('url', '')})\n{item.text}"
                    for item in chunks
                )
                citations = [
                    Citation(doc_id=item.doc_id, title=item.title, metadata=item.metadata)
                    for item in chunks
                ]

        system = SystemMessage(
            content=_system_prompt(
                bool(retrieved_block),
                request.rag.enabled,
                request.system_prompt,
            )
        )
        lc_messages: list[SystemMessage | HumanMessage | AIMessage] = [system]
        if retrieved_block:
            lc_messages.append(SystemMessage(content=f"检索到的资料：\n{retrieved_block}"))
        for message in request.messages:
            if message.role == "user":
                lc_messages.append(HumanMessage(content=message.content))
            elif message.role == "assistant":
                lc_messages.append(AIMessage(content=message.content))
            else:
                lc_messages.append(SystemMessage(content=message.content))

        return _PreparedChat(
            model=model,
            messages=lc_messages,
            citations=citations,
            session_id=request.session_id,
        )


def _sse(event: str, data: dict[str, Any]) -> str:
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


def _chunk_text(chunk: object) -> str:
    content = getattr(chunk, "content", chunk)
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, str):
                parts.append(item)
            elif isinstance(item, dict) and item.get("type") == "text":
                parts.append(str(item.get("text") or ""))
        return "".join(parts)
    return "" if content is None else str(content)


def _last_user_text(messages: list[ChatMessage]) -> str:
    for message in reversed(messages):
        if message.role == "user":
            return message.content.strip()
    return ""


def _system_prompt(has_sources: bool, rag_requested: bool, extra: str) -> str:
    base = (
        "你是接入本系统的助手，可以闲聊，也可以根据资料介绍内容。"
        "不能修改内容、不能执行管理操作、不能索要密钥。"
    )
    persona = extra.strip()
    if persona:
        base = base + " " + persona
    if has_sources:
        return (
            base
            + "下面提供了检索资料。回答站内问题时必须依据资料，并提到对应标题。"
            "不要编造资料中没有的文章。"
        )
    if rag_requested:
        return base + "这次没有检索到相关资料。如果用户在问站内文章，明确说没有找到，不要编造。"
    return base
