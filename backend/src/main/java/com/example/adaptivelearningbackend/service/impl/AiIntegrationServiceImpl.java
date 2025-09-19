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

    public AiIntegrationServiceImpl(@Value("${python.service.baseurl}") String pythonServiceBaseUrl,
                                    WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(pythonServiceBaseUrl).build();
        logger.info("Python AI Service Base URL: {}", pythonServiceBaseUrl);
    }

    @Override
    public LearningPathDTO generateLearningPath(String domainName,
                                                Long userId,
                                                TopicPerformanceDataDTO assessmentPerformanceData) {
        String endpoint = "/generate-learning-path";
        Map<String, Object> requestBody = Map.of(
                "domain_name", domainName,
                "user_id", userId,
                "user_topic_performance_data", buildUserTopicPerformancePayload(assessmentPerformanceData)
        );

        logger.debug("Sending request to Python AI for learning path: {} with body {}", endpoint, requestBody);

        try {
            LearningPathDTO response = postForObject(endpoint, requestBody, LearningPathDTO.class);
            if (response == null) {
                logger.warn("Python AI service returned an empty learning path for domain {}", domainName);
                return buildFallbackLearningPath(domainName);
            }
            return response;
        } catch (Exception e) {
            logger.error("Error calling Python AI service for learning path generation: {}", e.getMessage(), e);
            return buildFallbackLearningPath(domainName);
        }
    }

    @Override
    public List<InsightGenerationRequestDTO.InsightDetailDTO> generateInsightsForTopic(String domainName,
                                                                                       String topicName,
                                                                                       int level,
                                                                                       Long userId,
                                                                                       TopicPerformanceDataDTO userPerformanceData) {
        String endpoint = "/generate-insights";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("domain_name", domainName);
        requestBody.put("topic_name", topicName);
        requestBody.put("level", level);
        requestBody.put("user_id", userId);
        requestBody.put("user_topic_performance_data", buildUserTopicPerformancePayload(userPerformanceData));

        logger.debug("Sending request to Python AI for insights generation: {} with body {}", endpoint, requestBody);

        try {
            List<InsightGenerationRequestDTO.InsightDetailDTO> insights =
                    postForList(endpoint, requestBody, InsightGenerationRequestDTO.InsightDetailDTO.class);
            return insights != null ? insights : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error calling Python AI service for insight generation: {}", e.getMessage(), e);
            logger.warn("Returning empty list of insights due to error for topic: {}", topicName);
            return Collections.emptyList();
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
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("topic_progress_id", topicProgressId);
        requestBody.put("performance_data", performanceData != null ? performanceData : Collections.emptyMap());

        logger.debug("Sending request to Python AI for review generation: {} with body {}", endpoint, requestBody);

        try {
            ReviewDTO response = postForObject(endpoint, requestBody, ReviewDTO.class);
            if (response == null) {
                logger.warn("Python AI service returned no review for topic progress {}", topicProgressId);
                return buildFallbackReview();
            }
            return response;
        } catch (Exception e) {
            logger.error("Error calling Python AI service for review generation: {}", e.getMessage(), e);
            return buildFallbackReview();
        }
    }

    private <T> T postForObject(String endpoint, Object body, Class<T> responseType) {
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    private <T> List<T> postForList(String endpoint, Object body, Class<T> elementType) {
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(elementType)
                .collectList()
                .block();
    }

    private Map<String, Object> buildUserTopicPerformancePayload(TopicPerformanceDataDTO data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (data == null) {
            payload.put("assessment_answers", Collections.emptyList());
            payload.put("insights_performance", Collections.emptyList());
            return payload;
        }

        if (data.getUserId() != null) {
            payload.put("user_id", data.getUserId());
        }
        if (data.getDomainName() != null) {
            payload.put("domain_name", data.getDomainName());
        }
        if (data.getTopicName() != null) {
            payload.put("topic_name", data.getTopicName());
        }
        if (data.getCurrentLevel() != null) {
            payload.put("current_level", data.getCurrentLevel());
        }

        payload.put("assessment_answers", convertAssessmentAnswers(data.getAssessmentAnswers()));
        payload.put("insights_performance", convertInsightPerformance(data.getInsightsPerformance()));
        return payload;
    }

    private List<Map<String, Object>> convertAssessmentAnswers(List<RichAssessmentAnswerDTO> answers) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }

        return answers.stream()
                .map(answer -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("question_id", answer.getQuestionId());
                    map.put("question_text", answer.getQuestionText());
                    map.put("options", answer.getOptions() != null ? answer.getOptions() : Collections.emptyList());
                    map.put("selected_answer", answer.getSelectedAnswer());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> convertInsightPerformance(List<InsightPerformanceDataDTO> performances) {
        if (performances == null || performances.isEmpty()) {
            return Collections.emptyList();
        }

        return performances.stream()
                .map(performance -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("insight_id", performance.getInsightId());
                    map.put("insight_title", performance.getInsightTitle());
                    map.put("questions_answered", convertQuestionDetails(performance.getQuestionsAnswered()));
                    map.put("times_shown", performance.getTimesShown());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> convertQuestionDetails(List<UserAnswerDetailDTO> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }

        return details.stream()
                .map(detail -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("question_id", detail.getQuestionId());
                    map.put("question_text", detail.getQuestionText());
                    map.put("options", detail.getOptions() != null ? detail.getOptions() : Collections.emptyList());
                    map.put("selected_answer", detail.getSelectedAnswer());
                    map.put("correct_answer", detail.getCorrectAnswer());
                    map.put("is_correct", detail.isCorrect());
                    map.put("time_taken_ms", detail.getTimeTakenMs());
                    return map;
                })
                .toList();
    }


    private LearningPathDTO buildFallbackLearningPath(String domainName) {
        LearningPathDTO fallbackPath = new LearningPathDTO();
        fallbackPath.setDomainName(domainName);

        List<String> topics = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            topics.add(domainName + " Topic " + i);
        }
        fallbackPath.setTopics(topics);
        return fallbackPath;
    }

    private ReviewDTO buildFallbackReview() {
        return ReviewDTO.builder()
                .summary("A review summary could not be generated at this time. Please try again later.")
                .strengths(List.of("Persistence"))
                .weaknesses(List.of("Technical difficulties with AI review generation."))
                .revisionQuestions(new ArrayList<>())
                .build();
    }
}