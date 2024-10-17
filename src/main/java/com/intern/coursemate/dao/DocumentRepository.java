package com.intern.coursemate.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.intern.coursemate.model.Document;

import reactor.core.publisher.Flux;

@Repository
public interface DocumentRepository extends ReactiveCrudRepository<Document,Integer> {
    @Query(" SELECT d from Document d where d.sem = :sem and d.year = :year and d.subject = :subject")
    Flux<Document> fetchDocumentsByQuery(String year,int sem,String subject);
    Flux<Document> findByUserId(int userId);
    Flux<Document> findByUserId(int userId, Pageable pageable);
}

