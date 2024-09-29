package com.intern.coursemate.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "document",uniqueConstraints = @UniqueConstraint(name = "url",columnNames = "url"))
public class Document {
    @Id
    @GeneratedValue
    private int id;
    private String name;
    private String subject;
    private String year;
    private int sem;
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;
    private boolean isVerified;
    private String url;
}
