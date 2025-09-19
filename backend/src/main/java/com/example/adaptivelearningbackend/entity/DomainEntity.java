package com.example.adaptivelearningbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "domains")
public class DomainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "domain_seq")
    @SequenceGenerator(name = "domain_seq", sequenceName = "domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., Finance, Creative Writing

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;      // e.g. “Technology & Computer Science”

    // Pre-defined assessment questions for this domain
    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AssessmentQuestionEntity> assessmentQuestions = new ArrayList<>();
}