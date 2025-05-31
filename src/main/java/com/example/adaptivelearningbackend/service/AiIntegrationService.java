package com.example.adaptivelearningbackend.service;

import com.example.adaptivelearningbackend.dto.*;

import java.util.List;
import java.util.Map;

public interface AiIntegrationService {

    // Request to generate a learning path
    LearningPathDTO generateLearningPath(String domainName, Long userId, TopicPerformanceDataDTO assessmentPerformanceData);

    // Request to generate insights for a given topic
/*
    List<InsightGenerationRequestDTO.InsightDetailDTO> generateInsightsForTopic(String domainName, String topicName, int level, Long userId, Map<String, Object> userPerformanceMetrics);
*/

    // Request LlamaIndex to choose the next best insight
    // Needs current uncompleted insights, user history for this topic, etc.
/*
    NextInsightDTO getNextInsight(Long userId, Long topicProgressId, List<SimpleInsightInfoDTO> uncompletedInsights);
*/

    // Request to generate a review summary and questions
    ReviewDTO generateReview(Long userId, Long topicProgressId, Map<String, Object> performanceData);
    List<InsightGenerationRequestDTO.InsightDetailDTO> generateInsightsForTopic(
            String domainName, String topicName, int level, Long userId,
            TopicPerformanceDataDTO userPerformanceData // Changed from Map<String, Object>
    );
}