from fastapi import APIRouter, HTTPException, status

from agent.schemas.chat import ChatRequest

router = APIRouter()


@router.post("/chat")
def chat(_body: ChatRequest) -> None:
    raise HTTPException(
        status_code=status.HTTP_501_NOT_IMPLEMENTED,
        detail="chat and rag are not wired yet",
    )
