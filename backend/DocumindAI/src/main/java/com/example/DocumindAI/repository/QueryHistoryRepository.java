package com.example.DocuMindAI.repository;

import com.example.DocuMindAI.model.QueryHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends MongoRepository<QueryHistory, String> {
    List<QueryHistory> findByDocumentId(String documentId);
}