package com.intern.coursemate.dto;

import java.util.List;

import com.intern.coursemate.model.Document;
import com.intern.coursemate.model.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserWithDocuments {
    private User user;
    private List<Document> documents;
}