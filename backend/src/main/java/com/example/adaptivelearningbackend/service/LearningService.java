package com.example.adaptivelearningbackend.service;

import com.example.adaptivelearningbackend.dto.*;
import java.util.List;

public interface LearningService {
    List<DomainDTO> getAllDomains();
    List<AssessmentQuestionDTO> getAssessmentQuestions(Long domainId);
    LearningPathDTO startDomainAndGetLearningPath(Long userId, AssessmentSubmissionDTO submission); // userId will come from security context
    InsightDTO getNextInsight(Long userId, Long domainId);
    AnswerFeedbackDTO submitAnswer(Long userId, AnswerSubmissionDTO submission);
    TopicProgressDTO getTopicProgress(Long userId, Long domainId);
    ReviewDTO getReview(Long userId, Long domainId);
    void completeReviewAndAdvance(Long userId, Long domainId, boolean satisfactoryPerformance);
    DomainOverviewDTO getDomainOverview(Long userId, Long domainId);
    void selectTopic(Long userId, Long domainId, int topicIndex);
    List<DomainStatusDTO> getDomainsWithStatus(Long userId);
    int countCompletedInsights(Long userId);
}