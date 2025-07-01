import os
import time
import logging
from typing import List, Dict, Tuple
from pathlib import Path

import chromadb
from chromadb.config import Settings
from sentence_transformers import SentenceTransformer
from anthropic import Anthropic

from src.services.document_processor import DocumentProcessor

logger = logging.getLogger(__name__)

class RAGService:
    """Handles Retrieval-Augmented Generation pipeline"""
    
    def __init__(
        self,
        chroma_db_path: str,
        anthropic_api_key: str,
        embedding_model: str = "sentence-transformers/all-MiniLM-L6-v2",
        claude_model: str = "claude-3-haiku-20240307"
    ):
        # Initialize ChromaDB
        self.chroma_client = chromadb.PersistentClient(
            path=chroma_db_path,
            settings=Settings(anonymized_telemetry=False,
            allow_reset=True)
        )
        
        # Initialize embedding model
        logger.info(f"Loading embedding model: {embedding_model}")
        self.embedding_model = SentenceTransformer(embedding_model)
        
        # Initialize Anthropic client
        self.anthropic_client = Anthropic(api_key=anthropic_api_key)
        self.claude_model = claude_model
        
        logger.info("RAG Service initialized successfully")
    
    def process_document(self, document_id: str, file_path: str) -> Dict[str, any]:
        """
        Process a document: extract text, chunk, embed, and store
        
        Returns:
            Dict with processing statistics
        """
        start_time = time.time()
        
        try:
            # Step 1: Extract text from document
            logger.info(f"Extracting text from {file_path}")
            extracted_data = DocumentProcessor.extract_text(file_path)
            text = extracted_data['text']
            page_count = extracted_data['page_count']
            
            # Step 2: Chunk the text
            logger.info("Chunking document")
            chunks = DocumentProcessor.chunk_text(
                text,
                chunk_size=int(os.getenv('CHUNK_SIZE', 500)),
                overlap=int(os.getenv('CHUNK_OVERLAP', 50))
            )
            
            # Step 3: Generate embeddings
            logger.info(f"Generating embeddings for {len(chunks)} chunks")
            chunk_texts = [chunk['text'] for chunk in chunks]
            embeddings = self.embedding_model.encode(chunk_texts).tolist()
            
            # Step 4: Store in ChromaDB
            collection_name = f"doc_{document_id}"
            logger.info(f"Creating ChromaDB collection: {collection_name}")
            
            # Delete collection if it exists
            try:
                self.chroma_client.delete_collection(name=collection_name)
            except:
                pass
            
            collection = self.chroma_client.create_collection(name=collection_name)
            
            # Prepare data for ChromaDB
            ids = [f"chunk_{i}" for i in range(len(chunks))]
            metadatas = [
                {
                    'chunk_index': chunk['chunk_index'],
                    'word_count': chunk['word_count'],
                    'document_id': document_id
                }
                for chunk in chunks
            ]
            
            # Add to collection
            collection.add(
                ids=ids,
                embeddings=embeddings,
                documents=chunk_texts,
                metadatas=metadatas
            )
            
            processing_time = time.time() - start_time
            
            logger.info(f"Document processed in {processing_time:.2f}s")
            
            return {
                'status': 'success',
                'chunkCount': len(chunks),
                'processingTime': processing_time,
                'pageCount': page_count,
                'message': f'Successfully processed {len(chunks)} chunks'
            }
            
        except Exception as e:
            logger.error(f"Error processing document: {e}")
            raise
    
    def query_document(self, document_id: str, query: str, top_k: int = 5) -> Dict[str, any]:
        """
        Query a document using RAG pipeline
        
        Args:
            document_id: ID of the document to query
            query: User's question
            top_k: Number of chunks to retrieve
        
        Returns:
            Dict with answer, sources, and metadata
        """
        start_time = time.time()
        
        try:
            # Step 1: Get collection
            collection_name = f"doc_{document_id}"
            collection = self.chroma_client.get_collection(name=collection_name)
            
            # Step 2: Generate query embedding
            logger.info(f"Generating query embedding for: {query}")
            query_embedding = self.embedding_model.encode([query])[0].tolist()
            
            # Step 3: Retrieve relevant chunks
            logger.info(f"Retrieving top {top_k} relevant chunks")
            results = collection.query(
                query_embeddings=[query_embedding],
                n_results=top_k
            )
            
            # Step 4: Build context from retrieved chunks
            contexts = results['documents'][0]
            metadatas = results['metadatas'][0]
            distances = results['distances'][0]
            
            # Build prompt
            context_text = "\n\n".join([
                f"[Chunk {i+1}]\n{context}"
                for i, context in enumerate(contexts)
            ])
            
            prompt = f"""You are a helpful AI assistant that answers questions based strictly on the provided document context.

Context from document:
{context_text}

Question: {query}

Instructions:
- Answer based ONLY on the information in the context above
- If the context doesn't contain enough information to answer, say so
- Be specific and cite which chunk(s) support your answer
- Keep your answer concise and focused

Answer:"""
            
            # Step 5: Call Claude API
            logger.info("Calling Claude API")
            message = self.anthropic_client.messages.create(
                model=self.claude_model,
                max_tokens=int(os.getenv('MAX_TOKENS', 500)),
                temperature=float(os.getenv('TEMPERATURE', 0.2)),
                messages=[
                    {"role": "user", "content": prompt}
                ]
            )
            
            answer = message.content[0].text
            
            # Step 6: Prepare response with citations
            sources = []
            for i, (metadata, distance) in enumerate(zip(metadatas, distances)):
                # Convert distance to similarity score (lower distance = higher similarity)
                similarity = 1 / (1 + distance)
                
                sources.append({
                    'page': metadata.get('chunk_index', 0) + 1,  # Use chunk as "page"
                    'snippet': contexts[i][:200] + "...",  # First 200 chars
                    'relevance': round(similarity, 3)
                })
            
            processing_time = time.time() - start_time
            
            # Calculate confidence based on top similarity score
            confidence = sources[0]['relevance'] if sources else 0.0
            
            logger.info(f"Query processed in {processing_time:.2f}s")
            
            return {
                'answer': answer,
                'sources': sources,
                'processingTime': processing_time,
                'confidence': confidence
            }
            
        except Exception as e:
            logger.error(f"Error querying document: {e}")
            raise