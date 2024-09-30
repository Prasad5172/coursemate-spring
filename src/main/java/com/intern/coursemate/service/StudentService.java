package com.intern.coursemate.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Service;

import com.intern.coursemate.dao.StudentRepository;
import com.intern.coursemate.model.Student;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;
    
    public Optional<Student> findUserById(int id) {
        return studentRepository.findById(id);
    }
}
