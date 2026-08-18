from fastapi import APIRouter, HTTPException, status

from agent.schemas.document import DocumentResponse, DocumentUpsertRequest
from agent.services.document_store import InMemoryDocumentStore, StoredDocument

router = APIRouter()
store = InMemoryDocumentStore()


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
) -> DocumentResponse:
    stored = store.upsert(
        StoredDocument(
            project_id=project_id,
            doc_id=doc_id,
            corpus=body.corpus,
            title=body.title,
            text=body.text,
            content_type=body.content_type,
            metadata=body.metadata,
        )
    )
    return _to_response(stored)


@router.get("/projects/{project_id}/documents/{doc_id}", response_model=DocumentResponse)
def get_document(project_id: str, doc_id: str) -> DocumentResponse:
    stored = store.get(project_id, doc_id)
    if stored is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="document not found")
    return _to_response(stored)


@router.delete("/projects/{project_id}/documents/{doc_id}", status_code=204)
def delete_document(project_id: str, doc_id: str) -> None:
    store.delete(project_id, doc_id)
