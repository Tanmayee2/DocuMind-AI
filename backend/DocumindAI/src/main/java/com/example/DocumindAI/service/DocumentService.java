package com.example.DocuMindAI.service;

import com.example.DocuMindAI.model.DocumentEntity;
import com.example.DocuMindAI.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final AIServiceClient aiServiceClient;
    
    @Value("${DocuMindAI.upload-dir}")
    private String uploadDir;
    
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    public DocumentEntity uploadDocument(MultipartFile file, String userId) throws IOException {
        // Validate file
        validateFile(file);
        
        // Generate unique document ID
        String documentId = "doc_" + UUID.randomUUID().toString();
        
        // Determine file extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = documentId + extension;
        
        // Save file to disk
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());
        
        log.info("File saved to: {}", filePath);
        
        // Create MongoDB record
        DocumentEntity document = new DocumentEntity();
        document.setId(documentId);
        document.setFileName(fileName);
        document.setOriginalFileName(originalFilename);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setUploadedAt(LocalDateTime.now());
        document.setUserId(userId);
        document.setStatus("uploaded");
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        documentRepository.save(document);
        
        // Process document asynchronously via AI service
        processDocumentAsync(documentId, filePath.toString());
        
        return document;
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }
        
        // Use Apache Tika for accurate MIME type detection
        Tika tika = new Tika();
        String detectedType;
        try {
            detectedType = tika.detect(file.getInputStream());
        }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to detect file type");
        }
        
        if (!ALLOWED_TYPES.contains(detectedType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Only PDF, DOCX, and TXT files are allowed"
            );
        }
    }
    
    private void processDocumentAsync(String documentId, String filePath) {
        // In production, use @Async or message queue
        // For simplicity, we'll call synchronously but update status
        new Thread(() -> {
            try {
                // Update status to processing
                DocumentEntity document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
                document.setStatus("processing");
                documentRepository.save(document);
                
                // Call AI service
                aiServiceClient.processDocument(documentId, filePath);
                
                // Update status to processed
                document.setStatus("processed");
                document.setUpdatedAt(LocalDateTime.now());
                documentRepository.save(document);
                
                log.info("Document {} processed successfully", documentId);
                
            } catch (Exception e) {
                log.error("Error processing document {}: {}", documentId, e.getMessage());
                
                // Update status to failed
                DocumentEntity document = documentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    document.setStatus("failed");
                    documentRepository.save(document);
                }
            }
        }).start();
    }
    
    public DocumentEntity getDocument(String documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
    }
    
    public List<DocumentEntity> listDocuments(String userId) {
        return documentRepository.findByUserId(userId);
    }
    
    public void deleteDocument(String documentId) {
        DocumentEntity document = getDocument(documentId);
        
        // Delete file from disk
        try {
            Path filePath = Paths.get(uploadDir, document.getFileName());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
        }
        
        // Delete from database
        documentRepository.deleteById(documentId);
        
        // TODO: Delete from ChromaDB via AI service
        //Test
    }
}