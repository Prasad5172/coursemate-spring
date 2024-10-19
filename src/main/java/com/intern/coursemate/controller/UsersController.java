package com.intern.coursemate.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intern.coursemate.model.User;
import com.intern.coursemate.service.UserService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UsersController {
    private final  UserService userService;

    @GetMapping("/me")
public Mono<ResponseEntity<User>> authenticateUser() {
    return ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> {
            Authentication authentication = securityContext.getAuthentication();
            User authenticatedUser = (User) authentication.getPrincipal();
            return ResponseEntity.ok(authenticatedUser);  
        })
        .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());  // Handle unauthenticated cases
}

    
    @GetMapping({"","/"})
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Flux<User>>> allUsers() {
        System.out.println("Fetching all users");
        Flux<User> users = userService.allUser();
        return Mono.just(ResponseEntity.ok(users)); // Wrap the Flux in a ResponseEntity and return as Mono
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<String>> adminEndpoint() {
        return Mono.just(ResponseEntity.ok("Admin access granted."));
    }

}
