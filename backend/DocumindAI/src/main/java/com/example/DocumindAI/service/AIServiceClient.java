package com.example.DocuMindAI.service;

import com.example.DocuMindAI.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${documind.ai-service.url}")
    private String aiServiceUrl;
    
    public void processDocument(String documentId, String filePath) {
        String url = aiServiceUrl + "/process-document";
        
        Map<String, String> request = new HashMap<>();
        request.put("documentId", documentId);
        request.put("filePath", filePath);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Document {} sent to AI service successfully", documentId);
            } else {
                throw new RuntimeException("AI service returned non-OK status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling AI service: {}", e.getMessage());
            throw new RuntimeException("Failed to process document", e);
        }
    }
    
    public QueryResponse queryDocument(String documentId, String query) {
        String url = aiServiceUrl + "/query";
        
        Map<String, String> request = new HashMap<>();
        request.put("documentId", documentId);
        request.put("query", query);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                
                // Convert response to QueryResponse DTO
                QueryResponse queryResponse = new QueryResponse();
                queryResponse.setAnswer((String) body.get("answer"));
                queryResponse.setProcessingTime((Double) body.get("processingTime"));
                queryResponse.setConfidence((Double) body.get("confidence"));
                
                // TODO: Parse citations/sources
                
                return queryResponse;
            } else {
                throw new RuntimeException("AI service returned non-OK status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error querying AI service: {}", e.getMessage());
            throw new RuntimeException("Failed to query document", e);
        }
    }
}