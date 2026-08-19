from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage

from agent.config import Settings
from agent.schemas.complete import CompleteRequest, CompleteResponse
from agent.services.models import resolve_chat_model


class CompleteService:
    """LangChain 补全：摘要与帮写，不走知识库。"""

    def __init__(self, chat_model: BaseChatModel | None, settings: Settings) -> None:
        self._chat_model = chat_model
        self._settings = settings

    def complete(self, request: CompleteRequest) -> CompleteResponse:
        model = resolve_chat_model(self._chat_model, request.llm, self._settings)
        messages = self._messages(request)
        result = model.invoke(messages)
        text = result.content if isinstance(result.content, str) else str(result.content)
        return CompleteResponse(text=text.strip(), finish_reason="stop")

    def _messages(self, request: CompleteRequest) -> list[SystemMessage | HumanMessage]:
        if request.scenario == "chat":
            return [
                SystemMessage(content="你是助手。直接回答用户，不要执行管理操作。"),
                HumanMessage(content=request.instruction or request.text),
            ]
        if request.scenario == "summarize":
            return [
                SystemMessage(
                    content=(
                        "你是中文写作助手。根据用户提供的正文写摘要，长度约 80 到 160 字。"
                        "只输出摘要正文，不要标题、不要引号、不要解释。"
                    )
                ),
                HumanMessage(content=request.text.strip() or "（空正文）"),
            ]

        mode = (request.mode or "continue").strip().lower()
        if mode == "titles":
            return [
                SystemMessage(
                    content="你是中文写作助手。只输出标题，每行一个，共 3 到 5 个，不要编号和解释。"
                ),
                HumanMessage(content=f"正文：\n{request.text}"),
            ]
        if mode in {"rewrite", "expand"}:
            return [
                SystemMessage(content="你是中文写作助手。只输出改写后的 Markdown，不要解释。"),
                HumanMessage(
                    content=(
                        f"全文上下文：\n{request.context}\n\n"
                        f"需要处理的段落：\n{request.text}\n\n"
                        f"用户指令：{request.instruction or '润色这段文字'}"
                    )
                ),
            ]
        if mode == "outline":
            return [
                SystemMessage(content="你是中文写作助手。按大纲写 Markdown 正文，不要解释。"),
                HumanMessage(
                    content=(
                        f"已有正文：\n{request.text}\n\n"
                        f"大纲或指令：{request.instruction or request.context}"
                    )
                ),
            ]
        return [
            SystemMessage(
                content="你是中文写作助手。只输出续写的 Markdown，不要重复已有正文，不要解释。"
            ),
            HumanMessage(
                content=(
                    f"已有正文：\n{request.text}\n\n"
                    f"用户指令：{request.instruction or '请继续写下去'}"
                )
            ),
        ]
