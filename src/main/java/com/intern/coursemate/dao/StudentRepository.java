package com.intern.coursemate.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intern.coursemate.model.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student,Integer> {
    
}
