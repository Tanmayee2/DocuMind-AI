import os
import logging
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

from src.models.schemas import (
    ProcessDocumentRequest,
    ProcessDocumentResponse,
    QueryRequest,
    QueryResponse,
    HealthResponse
)
from src.services.rag_service import RAGService

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="DocuMind AI Service",
    description="AI-powered document processing and Q&A service",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize RAG service
rag_service = RAGService(chroma_db_path=os.getenv('CHROMA_DB_PATH', '../data/chromadb'),
    anthropic_api_key=os.getenv('ANTHROPIC_API_KEY'),
    embedding_model=os.getenv('EMBEDDING_MODEL', 'sentence-transformers/all-MiniLM-L6-v2'),
    claude_model=os.getenv('CLAUDE_MODEL', 'claude-3-haiku-20240307')
)

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="healthy",
        service="DocuMind AI Service",
        version="1.0.0"
    )

@app.post("/process-document", response_model=ProcessDocumentResponse)
async def process_document(request: ProcessDocumentRequest):
    """
    Process a document: extract text, chunk, embed, and store in vector DB
    """
    try:
        logger.info(f"Processing document: {request.documentId}")
        
        result = rag_service.process_document(
            document_id=request.documentId,
            file_path=request.filePath
        )
        
        return ProcessDocumentResponse(**result)
        
    except FileNotFoundError as e:
        logger.error(f"File not found: {e}")
        raise HTTPException(status_code=404, detail=str(e))
    
    except ValueError as e:
        logger.error(f"Invalid file type: {e}")
        raise HTTPException(status_code=400, detail=str(e))
    
    except Exception as e:
        logger.error(f"Error processing document: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.post("/query", response_model=QueryResponse)
async def query_document(request: QueryRequest):
    """
    Query a document using RAG pipeline
    """
    try:
        logger.info(f"Querying document {request.documentId}: {request.query}")
        
        result = rag_service.query_document(
            document_id=request.documentId,
            query=request.query,
            top_k=5
        )
        
        return QueryResponse(**result)
        
    except Exception as e:
        logger.error(f"Error querying document: {e}")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)