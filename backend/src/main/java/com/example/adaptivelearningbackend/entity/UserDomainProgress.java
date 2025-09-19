package com.example.adaptivelearningbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_domain_progress")
public class UserDomainProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_domain_progress_seq")
    @SequenceGenerator(name = "user_domain_progress_seq", sequenceName = "user_domain_progress_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private DomainEntity domain;

    // Stores the JSON response from the AI agent representing the learning path
    @Column(columnDefinition = "TEXT")
    private String learningPathJson; // Sequence of topics

    @Column(name = "assessment_answers_json", columnDefinition = "TEXT") // Store assessment answers as JSON
    private String assessmentAnswersJson; // e.g., {"questionId1": "answer1", "questionId2": "answer2"}

    @OneToMany(mappedBy = "userDomainProgress", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TopicProgress> topicProgresses = new ArrayList<>();

    @Column(name = "current_topic_index", nullable = false, columnDefinition = "integer default 0")
    private int currentTopicIndex = 0; // Index in the learningPathJson

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}