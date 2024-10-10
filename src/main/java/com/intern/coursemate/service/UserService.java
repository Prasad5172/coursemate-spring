package com.intern.coursemate.service;

import org.springframework.stereotype.Service;

import com.intern.coursemate.dao.UserRepository;
import com.intern.coursemate.model.User;

import lombok.AllArgsConstructor;

import java.util.*;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> allUser(){
        List<User> users = userRepository.findAll();
        return users;
    }
}
