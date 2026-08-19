from fastapi import APIRouter, HTTPException, Request, status

from agent.exceptions import LlmNotConfiguredError
from agent.schemas.complete import CompleteRequest, CompleteResponse

router = APIRouter()


@router.post("/complete", response_model=CompleteResponse)
def complete(body: CompleteRequest, request: Request) -> CompleteResponse:
    try:
        return request.app.state.services.complete.complete(body)
    except LlmNotConfiguredError as error:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(error),
        ) from error
