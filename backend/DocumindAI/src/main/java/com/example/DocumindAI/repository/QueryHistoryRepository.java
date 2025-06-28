package com.example.DocumindAI.repository;

import com.example.DocumindAI.model.QueryHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends MongoRepository<QueryHistory, String> {
    List<QueryHistory> findByDocumentId(String documentId);
}