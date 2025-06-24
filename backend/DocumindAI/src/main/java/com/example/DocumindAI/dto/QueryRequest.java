package com.example.DocuMindAI.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryRequest {
    
    @NotBlank(message = "Document ID is required")
    private String documentId;
    
    @NotBlank(message = "Query text is required")
    private String query;
}