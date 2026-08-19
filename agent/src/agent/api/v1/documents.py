from fastapi import APIRouter, HTTPException, Request, status

from agent.schemas.document import DocumentResponse, DocumentUpsertRequest
from agent.services.container import AppServices
from agent.services.document_store import StoredDocument

router = APIRouter()


def _services(request: Request) -> AppServices:
    return request.app.state.services


def _to_response(stored: StoredDocument) -> DocumentResponse:
    return DocumentResponse(
        project_id=stored.project_id,
        doc_id=stored.doc_id,
        corpus=stored.corpus,
        title=stored.title,
        text=stored.text,
        content_type=stored.content_type,
        metadata=stored.metadata,
    )


@router.put("/projects/{project_id}/documents/{doc_id}", response_model=DocumentResponse)
def upsert_document(
    project_id: str,
    doc_id: str,
    body: DocumentUpsertRequest,
    request: Request,
) -> DocumentResponse:
    stored = StoredDocument(
        project_id=project_id,
        doc_id=doc_id,
        corpus=body.corpus,
        title=body.title,
        text=body.text,
        content_type=body.content_type,
        metadata=body.metadata,
    )
    services = _services(request)
    services.corpus.apply_embed(body.embed, services.settings)
    services.documents.upsert(stored)
    services.corpus.upsert(stored)
    return _to_response(stored)


@router.get("/projects/{project_id}/documents/{doc_id}", response_model=DocumentResponse)
def get_document(project_id: str, doc_id: str, request: Request) -> DocumentResponse:
    stored = _services(request).documents.get(project_id, doc_id)
    if stored is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="document not found")
    return _to_response(stored)


@router.delete("/projects/{project_id}/documents/{doc_id}", status_code=204)
def delete_document(project_id: str, doc_id: str, request: Request) -> None:
    services = _services(request)
    services.documents.delete(project_id, doc_id)
    services.corpus.delete(project_id, doc_id)
