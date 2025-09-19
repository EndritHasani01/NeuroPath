package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiLearningPathRequestDTO { // For Python's /api/ai/generate-learning-path
    private String domainName;
    private Long userId;
    private TopicPerformanceDataDTO userTopicPerformanceData;
}
