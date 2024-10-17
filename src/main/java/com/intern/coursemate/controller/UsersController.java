package com.intern.coursemate.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intern.coursemate.model.User;
import com.intern.coursemate.service.UserService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.*;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UsersController {
    private final  UserService userService;

    @GetMapping("/me")
    public ResponseEntity<Mono<User>> authenticateUser() {
        System.out.println("users/me");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Mono<User authenticateUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(authenticateUser);
    }
    
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Flux<User>> allUsers() {
        System.out.println("users");
        Flux<User> users = userService.allUser();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint() {
        return ResponseEntity.ok("Admin access granted.");
    }

}
