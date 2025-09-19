package com.example.adaptivelearningbackend.entity;

import com.example.adaptivelearningbackend.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questions")
public class QuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "question_seq")
    @SequenceGenerator(name = "question_seq", sequenceName = "question_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insight_id", nullable = false)
    private InsightEntity insight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType; // MULTIPLE_CHOICE, TRUE_FALSE

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @ElementCollection(fetch = FetchType.EAGER) // For multiple choice options
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options = new ArrayList<>(); // Option text

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Youtube_feedback", joinColumns = @JoinColumn(name = "question_id"))
    @MapKeyColumn(name = "option_key") // For multiple choice, this could be option index or text. For T/F, "true" or "false".
    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private Map<String, String> answerFeedbacks = new HashMap<>(); // Key: option, Value: feedback

    @Column(nullable = false)
    private String correctAnswer; // Can be option index/text for MC, "true"/"false" for T/F
}
