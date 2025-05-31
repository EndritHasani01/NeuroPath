package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Matches Python's TopicPerformanceData
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicPerformanceDataDTO {
    private Long userId;
    private String domainName;
    private String topicName;
    private Integer currentLevel;
    private List<RichAssessmentAnswerDTO> assessmentAnswers; // For initial assessment
    private List<InsightPerformanceDataDTO> insightsPerformance; // For topic-level adaptation
}
