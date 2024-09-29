package com.intern.coursemate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intern.coursemate.service.StudentService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/auth")
public class StudentController {
    @Autowired
    private  StudentService studentService;

    @GetMapping("/login")
    public String login(@RequestParam String param) {
        return new String();
    }
    
}
