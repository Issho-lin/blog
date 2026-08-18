from fastapi import APIRouter, HTTPException, status

from agent.schemas.complete import CompleteRequest

router = APIRouter()


@router.post("/complete")
def complete(_body: CompleteRequest) -> None:
    raise HTTPException(
        status_code=status.HTTP_501_NOT_IMPLEMENTED,
        detail="llm complete is not wired yet",
    )
