from pydantic import BaseModel, Field
from typing import List, Optional

class ProcessDocumentRequest(BaseModel):
    documentId: str = Field(..., description="Unique document identifier")
    filePath: str = Field(..., description="Path to the document file")

class ProcessDocumentResponse(BaseModel):
    status: str
    chunkCount: int
    processingTime: float
    message: str

class QueryRequest(BaseModel):
    documentId: str = Field(..., description="Document ID to query")
    query: str = Field(..., min_length=1, description="User's question")

class Citation(BaseModel):
    page: int
    snippet: str
    relevance: float

class QueryResponse(BaseModel):
    answer: str
    sources: List[Citation]
    processingTime: float
    confidence: float

class HealthResponse(BaseModel):
    status: str
    service: str
    version: str