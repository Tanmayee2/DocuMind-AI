package com.example.DocuMindAI.repository;

import com.example.DocuMindAI.model.DocumentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentEntity, String> {
    List<DocumentEntity> findByUserId(String userId);
    List<DocumentEntity> findByStatus(String status);
}