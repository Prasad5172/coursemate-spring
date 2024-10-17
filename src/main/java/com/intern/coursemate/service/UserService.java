package com.intern.coursemate.service;

import org.springframework.stereotype.Service;

import com.intern.coursemate.dao.DocumentRepository;
import com.intern.coursemate.dao.UserRepository;
import com.intern.coursemate.dto.UserWithDocuments;
import com.intern.coursemate.model.Document;
import com.intern.coursemate.model.User;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public Flux<User> allUser(){
        Flux<User> users = userRepository.findAll();
        return users;
    }

    public Mono<UserWithDocuments> getUserWithDocuments(int userId) {
        Mono<User> userMono = userRepository.findById(userId);
        Flux<Document> documentsFlux = documentRepository.findByUserId(userId);

        return userMono.zipWith(documentsFlux.collectList(), 
            (user, documents) -> new UserWithDocuments(user, documents));
    }
}
