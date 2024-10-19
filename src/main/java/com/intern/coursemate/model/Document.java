package com.intern.coursemate.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("document")
public class Document {
    @Id
    private int id;
    private String name;
    private String subject;
    private String year;
    private int sem;
    // foreign key reference
    private long userId;
    private boolean isVerified;
    private String url;
}
