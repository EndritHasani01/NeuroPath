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
@Table(name = "assessment_questions")
public class AssessmentQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assessment_question_seq")
    @SequenceGenerator(name = "assessment_question_seq", sequenceName = "assessment_question_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    // Could be simple text options, or more structured if needed
    @ElementCollection(fetch = FetchType.EAGER) // Keep it simple for now
    @CollectionTable(name = "assessment_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options = new ArrayList<>();
}
