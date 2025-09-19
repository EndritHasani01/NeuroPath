package com.example.adaptivelearningbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicProgressDTO {
    private String topicName;
    private int level;
    private int completedInsightsCount;
    private int totalInsightsInLevel; // This might be the 'requiredInsightsForLevelCompletion'
    private int totalGeneratedInsightsForTopic; // Total insights available for this topic (e.g., ~6)
    private boolean reviewAvailable;
}