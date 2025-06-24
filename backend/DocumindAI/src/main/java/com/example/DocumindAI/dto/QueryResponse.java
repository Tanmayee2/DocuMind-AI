package com.DocumindAI.dto;

import lombok.Data;
import java.util.List;

@Data
public class QueryResponse {
    private String answer;
    private List<Source> sources;
    private Double processingTime;
    private Double confidence;
    
    @Data
    public static class Source {
        private Integer page;
        private String snippet;
        private Double relevance;
    }
}