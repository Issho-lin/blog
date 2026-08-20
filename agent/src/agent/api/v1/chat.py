from fastapi import APIRouter, HTTPException, Request, status
from fastapi.responses import StreamingResponse
from starlette.concurrency import iterate_in_threadpool

from agent.exceptions import LlmNotConfiguredError
from agent.schemas.chat import ChatRequest, ChatResponse

router = APIRouter()


@router.post("/chat", response_model=None)
async def chat(body: ChatRequest, request: Request) -> ChatResponse | StreamingResponse:
    services = request.app.state.services
    try:
        if body.stream:
            return StreamingResponse(
                iterate_in_threadpool(services.chat.stream(body)),
                media_type="text/event-stream",
                headers={
                    "Cache-Control": "no-cache, no-transform",
                    "Connection": "keep-alive",
                    "X-Accel-Buffering": "no",
                },
            )
        return services.chat.chat(body)
    except LlmNotConfiguredError as error:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(error),
        ) from error
