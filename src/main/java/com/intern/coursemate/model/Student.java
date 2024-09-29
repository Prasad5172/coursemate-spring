package com.intern.coursemate.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "student",uniqueConstraints = @UniqueConstraint(name = "email_id_unique",columnNames = "email"))
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    @Column(nullable = false)
    private String email;
    private String password;
    @Default
    private Boolean isVerified = false;
    @Default
    private Boolean isAdmin = false;
    @OneToMany(mappedBy = "student")
    private List<Document> documents;

}
