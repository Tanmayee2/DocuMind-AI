package com.documind.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "query_history")
public class QueryHistory {
    
    @Id
    private String id;
    
    private String documentId;
    private String query;
    private String response;
    private List<Citation> citations;
    private LocalDateTime timestamp;
    private Double responseTime;
    
    @Data
    public static class Citation {
        private String chunkId;
        private Integer pageNumber;
        private Double relevanceScore;
    }
}