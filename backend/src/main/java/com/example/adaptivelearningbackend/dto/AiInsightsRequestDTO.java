package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightsRequestDTO { // For Python's /api/ai/generate-insights
    private String domainName;
    private String topicName;
    private int level;
    private Long userId;
    private TopicPerformanceDataDTO userTopicPerformanceData;
}
