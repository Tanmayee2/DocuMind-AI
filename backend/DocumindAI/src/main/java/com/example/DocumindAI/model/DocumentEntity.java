package com.example.DocuMindAI.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "documents")
public class DocumentEntity {
    
    @Id
    private String id;
    
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadedAt;
    private String userId;
    private String status; // uploaded, processing, processed, failed
    
    private Map<String, Object> metadata;
    private String chromaCollectionId;
    private String extractedTextPath;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}