package com.example.DocuMindAI.controller;

import com.example.DocuMindAI.dto.QueryRequest;
import com.example.DocuMindAI.dto.QueryResponse;
import com.example.DocuMindAI.service.QueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {
    
    private final QueryService queryService;
    
    @PostMapping
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryService.processQuery(request);
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("Document not ready: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
            
        } catch (RuntimeException e) {
            log.error("Error processing query: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}