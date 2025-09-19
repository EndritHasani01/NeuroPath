package com.example.adaptivelearningbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topic_progress")
public class TopicProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "topic_progress_seq")
    @SequenceGenerator(name = "topic_progress_seq", sequenceName = "topic_progress_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_domain_progress_id", nullable = false)
    private UserDomainProgress userDomainProgress;

    @Column(nullable = false)
    private String topicName; // e.g., "Introduction to Budgeting"

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int level = 1; // e.g., level 1, level 2 within a topic

    @Column(name = "insights_generated", nullable = false, columnDefinition = "boolean default false")
    private boolean insightsGenerated = false;

    @OneToMany(mappedBy = "topicProgress",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Builder.Default
    private List<InsightEntity> insights = new ArrayList<>();

    @Column(name = "completed_insights_count", nullable = false, columnDefinition = "integer default 0")
    private int completedInsightsCount = 0;

    @Column(name = "required_insights_for_level_completion", nullable = false, columnDefinition = "integer default 6")
    private int requiredInsightsForLevelCompletion = 6; // Default to 6

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastReviewedAt;
}