from fastapi import APIRouter, HTTPException, Request, status

from agent.exceptions import LlmNotConfiguredError
from agent.schemas.chat import ChatRequest, ChatResponse

router = APIRouter()


@router.post("/chat", response_model=ChatResponse)
def chat(body: ChatRequest, request: Request) -> ChatResponse:
    try:
        return request.app.state.services.chat.chat(body)
    except LlmNotConfiguredError as error:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(error),
        ) from error
