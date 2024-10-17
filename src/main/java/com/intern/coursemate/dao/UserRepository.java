package com.intern.coursemate.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.intern.coursemate.model.User;

import reactor.core.publisher.Mono;





@Repository
public interface UserRepository extends ReactiveCrudRepository<User,Integer> {
    Mono<User> findByEmail(String email);
    Mono<User> findByName(String name);
}
