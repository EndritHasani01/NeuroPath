package com.example.adaptivelearningbackend.service.impl;

import com.example.adaptivelearningbackend.dto.*;
import com.example.adaptivelearningbackend.service.AiIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AiIntegrationServiceImpl implements AiIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(AiIntegrationServiceImpl.class);
    private final WebClient webClient;

    public AiIntegrationServiceImpl(@Value("${python.service.baseurl}") String pythonServiceBaseUrl, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(pythonServiceBaseUrl).build();
        logger.info("Python AI Service Base URL: {}", pythonServiceBaseUrl);
    }

    @Override
    public LearningPathDTO generateLearningPath(String domainName, Long userId, TopicPerformanceDataDTO assessmentPerformanceData) {
        String endpoint = "/generate-learning-path"; // Ensure this matches Python's endpoint
        // Convert List<RichAssessmentAnswerDTO> → List<Map<String,Object>> with snake_case keys

        List<Map<String, Object>> assessmentAnswersSnake = Collections.emptyList();

        if(!assessmentPerformanceData.getAssessmentAnswers().isEmpty()){
            assessmentAnswersSnake = assessmentPerformanceData.getAssessmentAnswers().stream()
                    .map(ans -> Map.<String, Object>of(
                            "question_id",     ans.getQuestionId(),
                            "question_text",   ans.getQuestionText(),
                            "options",         ans.getOptions(),
                            "selected_answer", ans.getSelectedAnswer()
                    ))
                    .toList();
        }


        List<Map<String, Object>> insightsPerformanceSnake = Collections.emptyList();

        if (assessmentPerformanceData.getInsightsPerformance() != null && !assessmentPerformanceData.getInsightsPerformance().isEmpty()) {
            insightsPerformanceSnake = assessmentPerformanceData.getInsightsPerformance().stream()
                    .map(ins -> {
                        List<Map<String, Object>> questionsAnsweredSnake = Collections.emptyList();
                        if (ins.getQuestionsAnswered() != null && !ins.getQuestionsAnswered().isEmpty()) {
                            questionsAnsweredSnake = ins.getQuestionsAnswered().stream()
                                    .map(q -> Map.<String, Object>of(
                                            "question_id",     q.getQuestionId(),
                                            "question_text",   q.getQuestionText(),
                                            "options",         q.getOptions(),
                                            "selected_answer", q.getSelectedAnswer(),
                                            "correct_answer",  q.getCorrectAnswer(),
                                            "is_correct",      q.isCorrect(),
                                            "time_taken_ms",   q.getTimeTakenMs()
                                    ))
                                    .toList();
                        }

                        return Map.<String, Object>of(
                                "insight_id",         ins.getInsightId(),
                                "insight_title",      ins.getInsightTitle(),
                                "questions_answered", questionsAnsweredSnake,
                                "times_shown",        ins.getTimesShown()
                        );
                    })
                    .toList();
        }

        Map<String, Object> userTopicPerfData = new HashMap<>();
        userTopicPerfData.put("user_id", assessmentPerformanceData.getUserId());
        userTopicPerfData.put("domain_name", assessmentPerformanceData.getDomainName());
        userTopicPerfData.put("topic_name", assessmentPerformanceData.getTopicName());
        userTopicPerfData.put("current_level", assessmentPerformanceData.getCurrentLevel());
        userTopicPerfData.put("assessment_answers", assessmentAnswersSnake);
        userTopicPerfData.put("insights_performance", insightsPerformanceSnake);

        Map<String, Object> requestBody = Map.of(
                "domain_name",                 domainName,
                "user_id",                     userId,
                "user_topic_performance_data", userTopicPerfData
        );

        logger.debug("Sending request to Python AI for learning path: {} with body {}", endpoint, requestBody);

        try {
            return webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(LearningPathDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error calling Python AI service for learning path generation: {}", e.getMessage(), e);
            LearningPathDTO fallbackPath = new LearningPathDTO();
            fallbackPath.setDomainName(domainName);
            fallbackPath.setTopics(Arrays.asList( // Ensure 10-20 topics as per Python constraint
                    domainName + " Topic 1", domainName + " Topic 2", domainName + " Topic 3",
                    domainName + " Topic 4", domainName + " Topic 5", domainName + " Topic 6",
                    domainName + " Topic 7", domainName + " Topic 8", domainName + " Topic 9",
                    domainName + " Topic 10"
            ));
            logger.warn("Returning fallback learning path for domain: {}", domainName);
            return fallbackPath;
        }
    }

    @Override
    public List<InsightGenerationRequestDTO.InsightDetailDTO> generateInsightsForTopic(
            String domainName, String topicName, int level, Long userId,
            TopicPerformanceDataDTO userPerformanceData
    ) {
        String endpoint = "/generate-insights";
        List<Map<String, Object>> assessmentAnswersSnake = Collections.emptyList();
        // Convert List<RichAssessmentAnswerDTO> → List<Map<String,Object>> with snake_case keys
        if(!userPerformanceData.getAssessmentAnswers().isEmpty()){{
            assessmentAnswersSnake = userPerformanceData.getAssessmentAnswers().stream()
                    .map(ans -> Map.<String, Object>of(
                            "question_id",     ans.getQuestionId(),
                            "question_text",   ans.getQuestionText(),
                            "options",         ans.getOptions(),
                            "selected_answer", ans.getSelectedAnswer()
                    ))
                    .toList();
        }}


        List<Map<String, Object>> insightsPerformanceSnake = Collections.emptyList();

        if (userPerformanceData.getInsightsPerformance() != null && !userPerformanceData.getInsightsPerformance().isEmpty()) {
            insightsPerformanceSnake = userPerformanceData.getInsightsPerformance().stream()
                    .map(ins -> {
                        List<Map<String, Object>> questionsAnsweredSnake = Collections.emptyList();
                        if (ins.getQuestionsAnswered() != null && !ins.getQuestionsAnswered().isEmpty()) {
                            questionsAnsweredSnake = ins.getQuestionsAnswered().stream()
                                    .map(q -> Map.<String, Object>of(
                                            "question_id",     q.getQuestionId(),
                                            "question_text",   q.getQuestionText(),
                                            "options",         q.getOptions(),
                                            "selected_answer", q.getSelectedAnswer(),
                                            "correct_answer",  q.getCorrectAnswer(),
                                            "is_correct",      q.isCorrect(),
                                            "time_taken_ms",   q.getTimeTakenMs()
                                    ))
                                    .toList();
                        }

                        return Map.<String, Object>of(
                                "insight_id",         ins.getInsightId(),
                                "insight_title",      ins.getInsightTitle(),
                                "questions_answered", questionsAnsweredSnake,
                                "times_shown",        ins.getTimesShown()
                        );
                    })
                    .toList();
        }


        Map<String, Object> userTopicPerfData = Map.of(
                "user_id",               userPerformanceData.getUserId(),
                "domain_name",           userPerformanceData.getDomainName(),
                "topic_name",            userPerformanceData.getTopicName(),
                "current_level",         userPerformanceData.getCurrentLevel(),
                "assessment_answers",    assessmentAnswersSnake,
                "insights_performance",  insightsPerformanceSnake
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("domain_name", domainName);
        requestBody.put("topic_name", topicName);
        requestBody.put("level", level);
        requestBody.put("user_id", userId);
        requestBody.put("user_topic_performance_data", userTopicPerfData);

        logger.debug("Sending request to Python AI for insights generation: {} with body {}", endpoint, requestBody);

        try {
            return webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(InsightGenerationRequestDTO.InsightDetailDTO.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            logger.error("Error calling Python AI service for insight generation: {}", e.getMessage(), e);
            logger.warn("Returning empty list of insights due to error for topic: {}", topicName);
            return new ArrayList<>();
        }
    }

    /*@Override
    public NextInsightDTO getNextInsight(Long userId, Long topicProgressId, List<SimpleInsightInfoDTO> uncompletedInsights) {
        // This method's call to Python doesn't change for now, Python's /get-next-insight is not LLM based in current code
        String endpoint = "/api/ai/get-next-insight";
        Map<String, Object> requestBody = Map.of(
                "user_id", userId,
                "topic_progress_id", topicProgressId,
                "uncompleted_insights", uncompletedInsights
        );
        logger.debug("Sending request to Python AI for next insight selection: {} with body {}", endpoint, requestBody);

        try {
            return webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(NextInsightDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error calling Python AI service for next insight selection: {}", e.getMessage(), e);
            if (uncompletedInsights != null && !uncompletedInsights.isEmpty()) {
                logger.warn("Returning first uncompleted insight as fallback.");
                return new NextInsightDTO(uncompletedInsights.get(0).getInsightId());
            }
            logger.error("No uncompleted insights to fallback to for next insight.");
            throw new RuntimeException("Failed to get next insight from AI and no fallback available.", e);
        }
    }*/

    @Override
    public ReviewDTO generateReview(Long userId, Long topicProgressId, Map<String, Object> performanceData) {
        String endpoint = "/generate-review";
        Map<String, Object> requestBody = Map.of(
                "user_id", userId,
                "topic_progress_id", topicProgressId,
                "performance_data", performanceData
        );
        logger.debug("Sending request to Python AI for review generation: {} with body {}", endpoint, requestBody);
        try {
            return webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ReviewDTO.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error calling Python AI service for review generation: {}", e.getMessage(), e);
            logger.warn("Returning dummy review due to error for topicProgressId: {}", topicProgressId);
            return ReviewDTO.builder() // Assuming ReviewDTO has a builder
                    .summary("A review summary could not be generated at this time. Please try again later.")
                    .strengths(List.of("Persistence"))
                    .weaknesses(List.of("Technical difficulties with AI review generation."))
                    .revisionQuestions(new ArrayList<>())
                    .build();
        }
    }
}