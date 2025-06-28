import os
import re
import time
from pathlib import Path
from typing import List, Dict
import logging

import PyPDF2
from docx import Document as DocxDocument

logger = logging.getLogger(__name__)

class DocumentProcessor:
    """Handles text extraction from various document formats"""
    
    @staticmethod
    def extract_text(file_path: str) -> Dict[str, any]:
        """
        Extract text from PDF, DOCX, or TXT files
        
        Returns:
            Dict with 'text', 'page_count', and 'metadata'
        """
        file_path = Path(file_path)
        
        if not file_path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")
        
        extension = file_path.suffix.lower()
        
        if extension == '.pdf':
            return DocumentProcessor._extract_pdf(file_path)
        elif extension == '.docx':
            return DocumentProcessor._extract_docx(file_path)
        elif extension == '.txt':
            return DocumentProcessor._extract_txt(file_path)
        else:
            raise ValueError(f"Unsupported file type: {extension}")
    
    @staticmethod
    def _extract_pdf(file_path: Path) -> Dict[str, any]:
        """Extract text from PDF"""
        try:
            with open(file_path, 'rb') as file:
                pdf_reader = PyPDF2.PdfReader(file)
                page_count = len(pdf_reader.pages)
                
                text_by_page = []
                for page_num in range(page_count):
                    page = pdf_reader.pages[page_num]
                    text = page.extract_text()
                    text_by_page.append({
                        'page_number': page_num + 1,
                        'text': text
                    })
                
                # Combine all text
                full_text = "\n\n".join([page['text'] for page in text_by_page])
                
                return {
                    'text': full_text,
                    'page_count': page_count,
                    'pages': text_by_page,
                    'metadata': {
                        'format': 'pdf',
                        'size': file_path.stat().st_size
                    }
                }
        except Exception as e:
            logger.error(f"Error extracting PDF: {e}")
            raise
    
    @staticmethod
    def _extract_docx(file_path: Path) -> Dict[str, any]:
        """Extract text from DOCX"""
        try:
            doc = DocxDocument(file_path)
            
            paragraphs = []
            for para in doc.paragraphs:
                if para.text.strip():
                    paragraphs.append(para.text)
            
            full_text = "\n\n".join(paragraphs)
            
            return {
                'text': full_text,
                'page_count': 1,  # DOCX doesn't have page concept
                'pages': [{'page_number': 1, 'text': full_text}],
                'metadata': {
                    'format': 'docx',
                    'paragraph_count': len(paragraphs),
                    'size': file_path.stat().st_size
                }
            }
        except Exception as e:
            logger.error(f"Error extracting DOCX: {e}")
            raise
    
    @staticmethod
    def _extract_txt(file_path: Path) -> Dict[str, any]:
        """Extract text from TXT"""
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                text = file.read()
            
            return {
                'text': text,
                'page_count': 1,
                'pages': [{'page_number': 1, 'text': text}],
                'metadata': {
                    'format': 'txt',
                    'size': file_path.stat().st_size
                }
            }
        except Exception as e:
            logger.error(f"Error extracting TXT: {e}")
            raise
    
    @staticmethod
    def chunk_text(text: str, chunk_size: int = 500, overlap: int = 50) -> List[Dict[str, any]]:
        """
        Split text into overlapping chunks
        
        Args:
            text: Full document text
            chunk_size: Number of words per chunk
            overlap: Number of overlapping words between chunks
        
        Returns:
            List of chunk dictionaries with text and metadata
        """
        # Split into words
        words = text.split()
        
        chunks = []
        start = 0
        chunk_index = 0
        
        while start < len(words):
            end = start + chunk_size
            chunk_words = words[start:end]
            chunk_text = ' '.join(chunk_words)
            
            # Try to end at sentence boundary
            if end < len(words):
                # Look for sentence endings in last 50 words
                last_words = ' '.join(chunk_words[-50:])
                sentence_end = max(
                    last_words.rfind('.'),
                    last_words.rfind('!'),
                    last_words.rfind('?')
                )
                
                if sentence_end != -1:
                    # Trim to sentence boundary
                    chunk_text = chunk_text[:len(chunk_text) - len(last_words) + sentence_end + 1]
            
            chunks.append({
                'chunk_index': chunk_index,
                'text': chunk_text.strip(),
                'start_word': start,
                'end_word': min(end, len(words)),
                'word_count': len(chunk_words)
            })
            
            start += chunk_size - overlap
            chunk_index += 1
        
        logger.info(f"Created {len(chunks)} chunks from {len(words)} words")
        return chunks