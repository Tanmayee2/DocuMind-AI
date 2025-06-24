package com.DocumindAI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentUploadResponse {
    private String documentId;
    private String status;
    private String message;
}