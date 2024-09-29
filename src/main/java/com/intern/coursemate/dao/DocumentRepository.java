package com.intern.coursemate.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.intern.coursemate.model.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document,Integer> {
    @Query(" SELECT d from Document d where d.sem = :sem and d.year = :year and d.subject = :subject")
    List<Document> fetchDocumentsByQuery(String year,int sem,String subject);
}

