package com.example.DocuMindAI.service;

import com.example.DocuMindAI.dto.QueryRequest;
import com.example.DocuMindAI.dto.QueryResponse;
import com.example.DocuMindAI.model.DocumentEntity;
import com.example.DocuMindAI.model.QueryHistory;
import com.example.DocuMindAI.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {
    
    private final DocumentService documentService;
    private final AIServiceClient aiServiceClient;
    private final QueryHistoryRepository queryHistoryRepository;
    
    public QueryResponse processQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        
        // Validate document exists and is processed
        DocumentEntity document = documentService.getDocument(request.getDocumentId());
        
        if (!"processed".equals(document.getStatus())) {
            throw new IllegalStateException(
                "Document is not ready for queries. Current status: " + document.getStatus()
            );
        }
        
        // Query AI service
        QueryResponse response = aiServiceClient.queryDocument(
            request.getDocumentId(), 
            request.getQuery()
        );
        
        // Calculate processing time
        long endTime = System.currentTimeMillis();
        double processingTime = (endTime - startTime) / 1000.0;
        response.setProcessingTime(processingTime);
        
        // Save to query history
        saveQueryHistory(request, response, processingTime);
        
        return response;
    }
    
    private void saveQueryHistory(QueryRequest request, QueryResponse response, double processingTime) {
        QueryHistory history = new QueryHistory();
        history.setDocumentId(request.getDocumentId());
        history.setQuery(request.getQuery());
        history.setResponse(response.getAnswer());
        history.setTimestamp(LocalDateTime.now());
        history.setResponseTime(processingTime);
        
        // TODO: Add citations from response
        
        queryHistoryRepository.save(history);
    }
}