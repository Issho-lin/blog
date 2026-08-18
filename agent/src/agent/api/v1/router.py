from fastapi import APIRouter, Depends

from agent.api.v1 import chat, complete, documents
from agent.security import require_api_key

router = APIRouter(prefix="/v1", dependencies=[Depends(require_api_key)])
router.include_router(documents.router)
router.include_router(complete.router)
router.include_router(chat.router)
