import uvicorn
from fastapi import FastAPI

from agent.api.health import router as health_router
from agent.api.v1.router import router as v1_router
from agent.config import get_settings


def create_app() -> FastAPI:
    application = FastAPI(
        title="Agent Platform",
        version="0.1.0",
        description="与业务项目解耦的 AI 平台：文档入库、补全、对话。",
    )
    application.include_router(health_router)
    application.include_router(v1_router)
    return application


app = create_app()


def main() -> None:
    settings = get_settings()
    uvicorn.run(
        "agent.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
    )
