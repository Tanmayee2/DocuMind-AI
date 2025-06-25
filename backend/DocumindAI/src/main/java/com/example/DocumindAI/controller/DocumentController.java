package com.example.DocuMindAI.controller;

import com.example.DocuMindAI.dto.DocumentUploadResponse;
import com.example.DocuMindAI.model.DocumentEntity;
import com.example.DocuMindAI.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", defaultValue = "default_user") String userId
    ) {
        try {
            DocumentEntity document = documentService.uploadDocument(file, userId);
            
            DocumentUploadResponse response = new DocumentUploadResponse(
                document.getId(),
                document.getStatus(),
                "Document uploaded successfully and is being processed"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                new DocumentUploadResponse(null, "error", e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new DocumentUploadResponse(null, "error", "Failed to upload document")
            );
        }
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentEntity> getDocument(@PathVariable String documentId) {
        try {
            DocumentEntity document = documentService.getDocument(documentId);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/list")
    public ResponseEntity<List<DocumentEntity>> listDocuments(
            @RequestParam(value = "userId", defaultValue = "default_user") String userId
    ) {
        List<DocumentEntity> documents = documentService.listDocuments(userId);
        return ResponseEntity.ok(documents);
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}