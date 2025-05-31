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
@Table(name = "insights")
public class InsightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insight_seq")
    @SequenceGenerator(name = "insight_seq", sequenceName = "insight_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_progress_id", nullable = false)
    private TopicProgress topicProgress;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title; // The insight itself

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation; // Less than 1 min to read

    @Column(name = "ai_metadata", columnDefinition = "TEXT") // Store as JSON string
    private String aiMetadata; // Other metadata important for AI agent and LlamaIndex

    @Column(name = "is_completed", nullable = false, columnDefinition = "boolean default false")
    private boolean completed = false;

    @Column(nullable = false)
    private int level;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @OneToMany(mappedBy = "insight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) // Questions are part of an insight
    private List<QuestionEntity> questions = new ArrayList<>();

    // For LlamaIndex or other selection strategies
    private double relevanceScore; // Could be updated by AI
    private int timesShown = 0;
}
