package com.example.adaptivelearningbackend.service.impl;

import com.example.adaptivelearningbackend.dto.*;
import com.example.adaptivelearningbackend.entity.*;
import com.example.adaptivelearningbackend.enums.QuestionType;
import com.example.adaptivelearningbackend.exception.NotFoundException;
import com.example.adaptivelearningbackend.repository.*;
import com.example.adaptivelearningbackend.service.AiIntegrationService;
import com.example.adaptivelearningbackend.service.LearningService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private static final Logger logger = LoggerFactory.getLogger(LearningServiceImpl.class);

    private final DomainRepository domainRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final UserDomainProgressRepository userDomainProgressRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final InsightRepository insightRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final AiIntegrationService aiIntegrationService;
    private final ObjectMapper objectMapper; // For JSON processing


    private static final int DEFAULT_INSIGHTS_PER_LEVEL_COMPLETION = 6;



    @Override
    @Transactional(readOnly = true)
    public List<DomainDTO> getAllDomains() {
        return domainRepository.findAll().stream()
                .map(this::mapToDomainDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentQuestionDTO> getAssessmentQuestions(Long domainId) {
        if (!domainRepository.existsById(domainId)) {
            throw new NotFoundException("Domain not found with ID: " + domainId);
        }
        return assessmentQuestionRepository.findByDomainId(domainId).stream()
                .map(this::mapToAssessmentQuestionDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LearningPathDTO startDomainAndGetLearningPath(Long userId, AssessmentSubmissionDTO submission) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        DomainEntity domain = domainRepository.findById(submission.getDomainId())
                .orElseThrow(() -> new NotFoundException("Domain not found with ID: " + submission.getDomainId()));

        Optional<UserDomainProgress> existingProgressOpt = userDomainProgressRepository.findByUserIdAndDomainId(userId, submission.getDomainId());

        if (existingProgressOpt.isPresent() && existingProgressOpt.get().getLearningPathJson() != null) {
            logger.info("User {} already has a learning path for domain {}. Returning existing one.", userId, domain.getName());
            return parseLearningPathJson(existingProgressOpt.get().getLearningPathJson());
        }

        List<RichAssessmentAnswerDTO> richAssessmentAnswers = mapAssessmentSubmissionToRichDTOs(submission.getAnswers());

        TopicPerformanceDataDTO assessmentPerformanceForAi = TopicPerformanceDataDTO.builder()
                .userId(userId)
                .domainName(domain.getName())
                .assessmentAnswers(richAssessmentAnswers)
                .insightsPerformance(Collections.emptyList())
                .build();

        LearningPathDTO learningPathDTO = aiIntegrationService.generateLearningPath(
                domain.getName(),
                userId,
                assessmentPerformanceForAi
        );

        UserDomainProgress progress = existingProgressOpt.orElseGet(() -> UserDomainProgress.builder()
                .user(user)
                .domain(domain)
                .startedAt(LocalDateTime.now())
                .build());

        try {
            progress.setAssessmentAnswersJson(objectMapper.writeValueAsString(submission.getAnswers()));
            progress.setLearningPathJson(objectMapper.writeValueAsString(learningPathDTO));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing learning path or assessment answers to JSON for user {} domain {}", userId, domain.getName(), e);
            throw new RuntimeException("Failed to save learning path due to JSON processing error.");
        }
        progress.setCurrentTopicIndex(0);
        userDomainProgressRepository.save(progress);

        if (learningPathDTO.getTopics() != null && !learningPathDTO.getTopics().isEmpty()) {
            String firstTopicName = learningPathDTO.getTopics().get(0);
            TopicPerformanceDataDTO initialTopicPerformanceForAi = TopicPerformanceDataDTO.builder()
                    .userId(userId)
                    .domainName(learningPathDTO.getDomainName())
                    .topicName(firstTopicName)
                    .currentLevel(1)
                    .assessmentAnswers(richAssessmentAnswers)
                    .insightsPerformance(Collections.emptyList())
                    .build();
            ensureTopicProgressExistsAndGenerateInsights(progress, learningPathDTO.getDomainName(), firstTopicName, 1, initialTopicPerformanceForAi);
        }
        return learningPathDTO;
    }

    private TopicPerformanceDataDTO gatherInsightPerformanceData(Long userId, String domainName, String topicName, int level, TopicProgress relevantTopicProgress) {
        List<InsightPerformanceDataDTO> insightPerformances = new ArrayList<>();
        List<InsightEntity> insightsOfTopicLevel = insightRepository.findByTopicProgressId(relevantTopicProgress.getId());

        for (InsightEntity insight : insightsOfTopicLevel) {
            List<UserAnswerDetailDTO> userAnswerDetails = new ArrayList<>();
            // Fetch answers specifically for this insight and user
            List<UserAnswer> userAnswersForInsight = userAnswerRepository.findByUserIdAndQuestionInsightId(userId, insight.getId());

            for (UserAnswer userAnswer : userAnswersForInsight) {
                QuestionEntity question = userAnswer.getQuestion();
                if (question == null) continue;

                userAnswerDetails.add(UserAnswerDetailDTO.builder()
                        .questionId(question.getId())
                        .questionText(question.getQuestionText())
                        .options(new ArrayList<>(question.getOptions()))
                        .selectedAnswer(userAnswer.getSelectedAnswer())
                        .correctAnswer(question.getCorrectAnswer())
                        .isCorrect(userAnswer.isCorrect())
                        .timeTakenMs(userAnswer.getTimeTakenMs())
                        .build());
            }

            if (!userAnswerDetails.isEmpty() || insight.getTimesShown() > 0) {
                insightPerformances.add(InsightPerformanceDataDTO.builder()
                        .insightId(insight.getId())
                        .insightTitle(insight.getTitle())
                        .questionsAnswered(userAnswerDetails)
                        .timesShown(insight.getTimesShown())
                        .build());
            }
        }

        return TopicPerformanceDataDTO.builder()
                .userId(userId)
                .domainName(domainName)
                .topicName(topicName)
                .currentLevel(level)
                .insightsPerformance(insightPerformances)
                .assessmentAnswers(Collections.emptyList())
                .build();
    }

    private TopicProgress ensureTopicProgressExistsAndGenerateInsights(UserDomainProgress userDomainProgress,
                                                                       String domainName, String topicName, int level,
                                                                       TopicPerformanceDataDTO performanceDataForAi) {
        TopicProgress topicProgress = topicProgressRepository
                .findByUserDomainProgressIdAndTopicNameAndLevel(userDomainProgress.getId(), topicName, level)
                .orElseGet(() -> {
                    logger.info("Creating new TopicProgress for user {}, topic {}, level {}.", userDomainProgress.getUser().getId(), topicName, level);
                    return topicProgressRepository.save(TopicProgress.builder()
                            .userDomainProgress(userDomainProgress)
                            .topicName(topicName)
                            .level(level)
                            .insightsGenerated(false)
                            .completedInsightsCount(0)
                            .requiredInsightsForLevelCompletion(DEFAULT_INSIGHTS_PER_LEVEL_COMPLETION)
                            .startedAt(LocalDateTime.now())
                            .build());
                });

        long insightsInDbForThisTopicLevel = insightRepository.countByTopicProgressId(topicProgress.getId());
        boolean needsGeneration = insightsInDbForThisTopicLevel < topicProgress.getRequiredInsightsForLevelCompletion()/* || performanceDataForAi != null*/;

        if (needsGeneration) {
            if (insightsInDbForThisTopicLevel > 0) {
                logger.info("Clearing {} existing insights for topic {}, level {} before adaptive regeneration.", insightsInDbForThisTopicLevel, topicName, level);
                List<InsightEntity> oldInsights = insightRepository.findByTopicProgressId(topicProgress.getId());
                if (!oldInsights.isEmpty()) {
                    for (InsightEntity oldInsight : oldInsights) {
                        userAnswerRepository.deleteByQuestionInsightId(oldInsight.getId());
                        questionRepository.deleteByInsightId(oldInsight.getId());
                    }
                    insightRepository.deleteAll(oldInsights);
                    topicProgress.getInsights().clear();
                    topicProgress.setCompletedInsightsCount(0);
                }
            }

            logger.info("Requesting AI to generate insights for user {}, topic {}, level {}.", userDomainProgress.getUser().getId(), topicName, level);
            List<InsightGenerationRequestDTO.InsightDetailDTO> generatedInsightDetails =
                    aiIntegrationService.generateInsightsForTopic(
                            domainName, topicName, level, userDomainProgress.getUser().getId(),
                            performanceDataForAi
                    );

            if (generatedInsightDetails.isEmpty() && DEFAULT_INSIGHTS_PER_LEVEL_COMPLETION > 0) {
                logger.warn("AI returned no insights for topic {}, level {}. This might be an issue.", topicName, level);
            }

            List<InsightEntity> newInsights = new ArrayList<>();
            for (InsightGenerationRequestDTO.InsightDetailDTO detailDTO : generatedInsightDetails) {
                InsightEntity insight = InsightEntity.builder()
                        .topicProgress(topicProgress)
                        .title(detailDTO.getTitle())
                        .explanation(detailDTO.getExplanation())
                        .aiMetadata(convertMapToJson(detailDTO.getAiMetadata()))
                        .level(level)
                        .completed(false)
                        .relevanceScore(0.5 + (Math.random() * 0.5))
                        .timesShown(0)
                        .build();

                List<QuestionEntity> questions = new ArrayList<>();
                if (detailDTO.getQuestions() != null) {
                    for (InsightGenerationRequestDTO.QuestionDetailDTO qDto : detailDTO.getQuestions()) {
                        questions.add(QuestionEntity.builder()
                                .insight(insight)
                                .questionType(qDto.getQuestionType())
                                .questionText(qDto.getQuestionText())
                                .options(qDto.getOptions() != null ? new ArrayList<>(qDto.getOptions()) : Collections.emptyList())
                                .correctAnswer(qDto.getCorrectAnswer())
                                .answerFeedbacks(qDto.getAnswerFeedbacks() != null ? new HashMap<>(qDto.getAnswerFeedbacks()) : Collections.emptyMap())
                                .build());
                    }
                }
                insight.setQuestions(questions);
                newInsights.add(insight);
            }
            insightRepository.saveAll(newInsights); // Batch save

            topicProgress.getInsights().addAll(newInsights);
            topicProgress.setRequiredInsightsForLevelCompletion(Math.max(newInsights.size(), DEFAULT_INSIGHTS_PER_LEVEL_COMPLETION));
            if (newInsights.isEmpty() && DEFAULT_INSIGHTS_PER_LEVEL_COMPLETION > 0) {
                topicProgress.setRequiredInsightsForLevelCompletion(0);
            } else {
                topicProgress.setRequiredInsightsForLevelCompletion(newInsights.size());
            }
        } else {
            logger.info("Insights already generated and meet requirements for topic {}, level {}. Count: {}", topicName, level, insightsInDbForThisTopicLevel);
        }
        topicProgress.setInsightsGenerated(true);
        return topicProgressRepository.save(topicProgress);
    }




    @Override
    @Transactional
    public InsightDTO getNextInsight(Long userId, Long domainId) {
        UserDomainProgress userDomainProgress = userDomainProgressRepository.findByUserIdAndDomainId(userId, domainId)
                .orElseThrow(() -> new NotFoundException("User progress not found for domain. Please start the domain first."));

        LearningPathDTO learningPath = parseLearningPathJson(userDomainProgress.getLearningPathJson());
        if (learningPath == null || learningPath.getTopics() == null || learningPath.getTopics().isEmpty()) {
            throw new NotFoundException("Learning path not defined or empty for this domain.");
        }

        String currentTopicName = learningPath.getTopics().get(userDomainProgress.getCurrentTopicIndex());
        TopicProgress currentTopicProgress = topicProgressRepository
                .findByUserDomainProgressIdAndTopicNameAndLevel(userDomainProgress.getId(), currentTopicName, getCurrentLevelForTopic(userDomainProgress, currentTopicName))
                .orElseGet(() -> ensureTopicProgressExistsAndGenerateInsights(userDomainProgress, learningPath.getDomainName(), currentTopicName, getCurrentLevelForTopic(userDomainProgress, currentTopicName)));


        if (!currentTopicProgress.isInsightsGenerated()) {
            ensureTopicProgressExistsAndGenerateInsights(userDomainProgress,learningPath.getDomainName(),currentTopicName,currentTopicProgress.getLevel()); // regenerate if somehow missed
        }

        List<InsightEntity> uncompletedInsights = insightRepository.findUncompletedInsightsForTopic(currentTopicProgress.getId());


        if (uncompletedInsights.isEmpty()) {

            logger.info("No uncompleted insights found for user {}, topic {}, level {}. Review might be available.", userId, currentTopicName, currentTopicProgress.getLevel());
            return null;
        }

        List<SimpleInsightInfoDTO> simpleInsights = uncompletedInsights.stream()
                .map(insight -> new SimpleInsightInfoDTO(insight.getId(), insight.getTitle(), insight.getRelevanceScore(), insight.getTimesShown()))
                .collect(Collectors.toList());

        //Note: use this when you make further additions to python agents, this time we will use insight in order
        //NextInsightDTO nextInsightChoice = aiIntegrationService.getNextInsight(userId, currentTopicProgress.getId(), simpleInsights);
        NextInsightDTO nextInsightChoice = null;
        if (nextInsightChoice == null || nextInsightChoice.getInsightId() == null) {
            logger.error("AI service did not return a next insight for user {}, topic {}", userId, currentTopicName);
            if(!uncompletedInsights.isEmpty()){
                nextInsightChoice = new NextInsightDTO(uncompletedInsights.get(0).getId());
                logger.warn("Falling back to first uncompleted insight ID: {}", nextInsightChoice.getInsightId());
            } else {
                throw new NotFoundException("No next insight could be determined, and no uncompleted insights available.");
            }
        }

        NextInsightDTO finalNextInsightChoice = nextInsightChoice;
        InsightEntity chosenInsight = insightRepository.findById(nextInsightChoice.getInsightId())
                .orElseThrow(() -> new NotFoundException("Chosen insight not found with ID: " + finalNextInsightChoice.getInsightId()));

        chosenInsight.setTimesShown(chosenInsight.getTimesShown() + 1);
        chosenInsight.setLastAccessedAt(LocalDateTime.now());
        insightRepository.save(chosenInsight);

        return mapToInsightDTO(chosenInsight);
    }

    @Override
    @Transactional
    public AnswerFeedbackDTO submitAnswer(Long userId, AnswerSubmissionDTO submission) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        QuestionEntity question = questionRepository.findById(submission.getQuestionId())
                .orElseThrow(() -> new NotFoundException("Question not found with ID: " + submission.getQuestionId()));

        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(submission.getSelectedAnswer());
        String feedbackText = question.getAnswerFeedbacks().getOrDefault(submission.getSelectedAnswer(),
                isCorrect ? "Correct!" : "Incorrect. The correct answer was " + question.getCorrectAnswer());


        UserAnswer userAnswer = UserAnswer.builder()
                .user(user)
                .question(question)
                .selectedAnswer(submission.getSelectedAnswer())
                .isCorrect(isCorrect)
                .timeTakenMs(submission.getTimeTakenMs())
                .answeredAt(LocalDateTime.now())
                .build();
        userAnswerRepository.save(userAnswer);

        InsightEntity insight = question.getInsight();
        if (!insight.isCompleted()) {
            long answersForThisInsight = userAnswerRepository.findByUserIdAndQuestionInsightId(userId, insight.getId()).stream()
                    .map(UserAnswer::getQuestion)
                    .distinct() // count distinct questions answered for this insight
                    .count();

            if (answersForThisInsight >= insight.getQuestions().size()) {
                insight.setCompleted(true);
                insightRepository.save(insight);
                logger.info("Insight {} marked as completed for user {}", insight.getId(), userId);

                TopicProgress topicProgress = insight.getTopicProgress();
                topicProgress.setCompletedInsightsCount(topicProgress.getCompletedInsightsCount() + 1);
                topicProgressRepository.save(topicProgress);
                logger.info("Topic {} progress updated: {} insights completed for user {}",
                        topicProgress.getTopicName(), topicProgress.getCompletedInsightsCount(), userId);
            }
        }


        return new AnswerFeedbackDTO(question.getId(), submission.getSelectedAnswer(), isCorrect, question.getCorrectAnswer(), feedbackText);
    }

    @Override
    @Transactional(readOnly = true)
    public TopicProgressDTO getTopicProgress(Long userId, Long domainId) {
        UserDomainProgress udp = userDomainProgressRepository.findByUserIdAndDomainId(userId, domainId)
                .orElseThrow(() -> new NotFoundException("User progress not found."));

        LearningPathDTO learningPath = parseLearningPathJson(udp.getLearningPathJson());
        if (learningPath == null || learningPath.getTopics().isEmpty()) {
            throw new NotFoundException("Learning path not available.");
        }
        String currentTopicName = learningPath.getTopics().get(udp.getCurrentTopicIndex());
        int currentLevel = getCurrentLevelForTopic(udp, currentTopicName);

        TopicProgress topicProgress = topicProgressRepository
                .findByUserDomainProgressIdAndTopicNameAndLevel(udp.getId(), currentTopicName, currentLevel)
                .orElseThrow(() -> new NotFoundException("Topic progress not found for " + currentTopicName + " level " + currentLevel));

        boolean reviewAvailable = topicProgress.getCompletedInsightsCount() >= topicProgress.getRequiredInsightsForLevelCompletion();

        return TopicProgressDTO.builder()
                .topicName(topicProgress.getTopicName())
                .level(topicProgress.getLevel())
                .completedInsightsCount(topicProgress.getCompletedInsightsCount())
                .totalInsightsInLevel(topicProgress.getRequiredInsightsForLevelCompletion())
                .totalGeneratedInsightsForTopic((int) insightRepository.findByTopicProgressId(topicProgress.getId()).stream().count())
                .reviewAvailable(reviewAvailable)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public ReviewDTO getReview(Long userId, Long domainId) {
        UserDomainProgress udp = userDomainProgressRepository
                .findByUserIdAndDomainId(userId, domainId)
                .orElseThrow(() -> new NotFoundException("User progress not found for this domain."));
        LearningPathDTO learningPath = parseLearningPathJson(udp.getLearningPathJson());
        String currentTopicName = learningPath.getTopics().get(udp.getCurrentTopicIndex());
        int currentLevel       = getCurrentLevelForTopic(udp, currentTopicName);

        TopicProgress topicProgress = topicProgressRepository
                .findByUserDomainProgressIdAndTopicNameAndLevel(
                        udp.getId(), currentTopicName, currentLevel)
                .orElseThrow(() -> new NotFoundException("Topic progress not found for review."));

        if (topicProgress.getCompletedInsightsCount() < topicProgress.getRequiredInsightsForLevelCompletion()) {
            throw new IllegalStateException("Review is not yet available. Not enough insights completed.");
        }
        TopicPerformanceDataDTO performanceDto =
                gatherInsightPerformanceData(userId, learningPath.getDomainName(),
                        currentTopicName, currentLevel, topicProgress);

        Map<String, Object> performanceDataForReview = new HashMap<>();
        performanceDataForReview.put("topicName", currentTopicName);
        performanceDataForReview.put("level",      currentLevel);
        performanceDataForReview.put("userId",     userId);
        performanceDataForReview.put("completedInsightsInLevel", topicProgress.getCompletedInsightsCount());

        long totalQuestionsAnswered = 0;
        long totalCorrectAnswers    = 0;

        List<Map<String, Object>> answeredQuestions = new ArrayList<>();

        if (performanceDto.getInsightsPerformance() != null) {
            for (InsightPerformanceDataDTO ipd : performanceDto.getInsightsPerformance()) {
                if (ipd.getQuestionsAnswered() == null) continue;

                for (UserAnswerDetailDTO a : ipd.getQuestionsAnswered()) {
                    totalQuestionsAnswered++;
                    if (a.isCorrect()) totalCorrectAnswers++;

                    Map<String, Object> q = new HashMap<>();
                    q.put("questionId",     a.getQuestionId());
                    q.put("questionText",   a.getQuestionText());
                    q.put("options",        a.getOptions());        // full MC list (empty for T/F)
                    q.put("selectedAnswer", a.getSelectedAnswer());
                    q.put("correctAnswer",  a.getCorrectAnswer());
                    q.put("isCorrect",      a.isCorrect());
                    q.put("timeTakenMs",    a.getTimeTakenMs());
                    answeredQuestions.add(q);
                }
            }
        }

        double accuracy = (totalQuestionsAnswered == 0)
                ? 0.0
                : (double) totalCorrectAnswers / totalQuestionsAnswered;

        performanceDataForReview.put("accuracy",              accuracy * 100); // %
        performanceDataForReview.put("totalQuestionsAnswered", totalQuestionsAnswered);
        performanceDataForReview.put("totalCorrectAnswers",    totalCorrectAnswers);
        performanceDataForReview.put("answeredQuestions",      answeredQuestions);

        ReviewDTO reviewDTO = aiIntegrationService.generateReview(
                userId, topicProgress.getId(), performanceDataForReview);

        List<QuestionDTO> completedQuestions = userAnswerRepository.findByUserId(userId).stream()
                .filter(ans -> ans.getQuestion().getInsight()
                        .getTopicProgress().getId().equals(topicProgress.getId()))
                .map(UserAnswer::getQuestion)
                .distinct()
                .map(this::mapToQuestionDTO)
                .collect(Collectors.toList());
        Collections.shuffle(completedQuestions);
        reviewDTO.setRevisionQuestions(
                completedQuestions.stream().limit(3).collect(Collectors.toList()));

        topicProgress.setLastReviewedAt(LocalDateTime.now());
        topicProgressRepository.save(topicProgress);

        return reviewDTO;
    }

    private String questionTextToConcept(String questionText) {
        if (questionText.toLowerCase().contains("what is")) {
            return questionText.substring(questionText.toLowerCase().indexOf("what is") + "what is".length()).replace("?", "").trim();
        }
        if (questionText.length() > 30) return questionText.substring(0, 30) + "...";
        return questionText;
    }

    private InsightGenerationRequestDTO.QuestionDetailDTO mapToQuestionDetailDTO(QuestionEntity q) {
        return InsightGenerationRequestDTO.QuestionDetailDTO.builder()
                .questionType(q.getQuestionType())
                .questionText(q.getQuestionText())
                .options(q.getOptions())
                .correctAnswer(q.getCorrectAnswer())
                .answerFeedbacks(q.getAnswerFeedbacks())
                .build();
    }


    @Override
    @Transactional
    public void completeReviewAndAdvance(Long userId, Long domainId, boolean satisfactoryPerformance) {
        UserDomainProgress udp = userDomainProgressRepository.findByUserIdAndDomainId(userId, domainId)
                .orElseThrow(() -> new NotFoundException("User progress not found."));
        LearningPathDTO learningPath = parseLearningPathJson(udp.getLearningPathJson());
        String currentTopicName = learningPath.getTopics().get(udp.getCurrentTopicIndex());
        int currentLevel = getCurrentLevelForTopic(udp, currentTopicName);
        TopicProgress currentTopicProgress = topicProgressRepository
                .findByUserDomainProgressIdAndTopicNameAndLevel(udp.getId(), currentTopicName, currentLevel)
                .orElseThrow(() -> new NotFoundException("Current topic progress not found."));

        // Gather performance data for the level just completed/reviewed
        TopicPerformanceDataDTO performanceDataFromCompletedLevel = gatherInsightPerformanceData(userId, learningPath.getDomainName(), currentTopicName, currentLevel, currentTopicProgress);

        if (!satisfactoryPerformance) {
            logger.info("User {} performance unsatisfactory for topic {} level {}. Will regenerate insights for reinforcement.", userId, currentTopicName, currentLevel);
            // Insights themselves are cleared and regenerated by ensureTopicProgress... if logic is correct
            currentTopicProgress.setCompletedInsightsCount(0); // Reset count for this level
            currentTopicProgress.setCompletedAt(null); // Not completed
            currentTopicProgress.setLastReviewedAt(LocalDateTime.now()); // Mark review time
            // Insights will be regenerated with adaptation based on performanceDataFromCompletedLevel
            ensureTopicProgressExistsAndGenerateInsights(udp, learningPath.getDomainName(), currentTopicName, currentLevel, performanceDataFromCompletedLevel);
            topicProgressRepository.save(currentTopicProgress);
            return;
        }

        // Satisfactory performance: Mark current level complete and advance
        currentTopicProgress.setCompletedAt(LocalDateTime.now());
        topicProgressRepository.save(currentTopicProgress);

        int nextLevel = currentLevel + 1;
        logger.info("User {} advancing to level {} for topic {}.", userId, nextLevel, currentTopicName);
        ensureTopicProgressExistsAndGenerateInsights(udp, learningPath.getDomainName(), currentTopicName, nextLevel, performanceDataFromCompletedLevel);

    }

    private TopicProgress ensureTopicProgressExistsAndGenerateInsights(UserDomainProgress userDomainProgress, String domainName, String topicName, int level) {
        logger.debug("ensureTopicProgressExistsAndGenerateInsights called without explicit performance data for {} L{}. Generating insights with default adaptation planning.", topicName, level);
        TopicPerformanceDataDTO defaultPerformanceData = TopicPerformanceDataDTO.builder()
                .userId(userDomainProgress.getUser().getId())
                .domainName(domainName)
                .topicName(topicName)
                .currentLevel(level)
                .insightsPerformance(Collections.emptyList())
                .assessmentAnswers(Collections.emptyList()) // No specific assessment answers for this context
                .build();
        return ensureTopicProgressExistsAndGenerateInsights(userDomainProgress, domainName, topicName, level, defaultPerformanceData);
    }

    // --- Helper Methods ---

    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error("Error converting AI metadata map to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> convertJsonToMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error converting JSON to AI metadata map", e);
            return Map.of();
        }
    }


    private LearningPathDTO parseLearningPathJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, LearningPathDTO.class);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing learning path JSON", e);
            return null;
        }
    }

    private int getCurrentLevelForTopic(UserDomainProgress udp, String topicName) {
        return udp.getTopicProgresses().stream()
                .filter(tp -> tp.getTopicName().equals(topicName))
                .mapToInt(TopicProgress::getLevel)
                .max()
                .orElse(1);
    }

    private double calculateAccuracyForTopic(Long userId, Long topicProgressId) {
        List<InsightEntity> insightsInTopic = insightRepository.findByTopicProgressId(topicProgressId);
        if (insightsInTopic.isEmpty()) return 0.0;

        List<Long> questionIdsInTopic = insightsInTopic.stream()
                .flatMap(i -> i.getQuestions().stream())
                .map(QuestionEntity::getId)
                .collect(Collectors.toList());

        if (questionIdsInTopic.isEmpty()) return 0.0;

        List<UserAnswer> answers = userAnswerRepository.findByUserId(userId).stream()
                .filter(ua -> questionIdsInTopic.contains(ua.getQuestion().getId()))
                .toList();

        if (answers.isEmpty()) return 0.0; // No answers yet for this topic

        long correctAnswers = answers.stream().filter(UserAnswer::isCorrect).count();
        return (double) correctAnswers / answers.size();
    }


    private DomainDTO mapToDomainDTO(DomainEntity entity) {
        DomainDTO dto = new DomainDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        return dto;
    }

    private AssessmentQuestionDTO mapToAssessmentQuestionDTO(AssessmentQuestionEntity entity) {
        AssessmentQuestionDTO dto = new AssessmentQuestionDTO();
        dto.setId(entity.getId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setOptions(new ArrayList<>(entity.getOptions()));
        return dto;
    }

    private InsightDTO mapToInsightDTO(InsightEntity entity) {
        return InsightDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .explanation(entity.getExplanation())
                .aiMetadata(convertJsonToMap(entity.getAiMetadata()))
                .completed(entity.isCompleted())
                .questions(entity.getQuestions().stream().map(this::mapToQuestionDTO).collect(Collectors.toList()))
                .build();
    }

    private QuestionDTO mapToQuestionDTO(QuestionEntity entity) {
        return QuestionDTO.builder()
                .id(entity.getId())
                .questionType(entity.getQuestionType())
                .questionText(entity.getQuestionText())
                .options(entity.getQuestionType() == QuestionType.MULTIPLE_CHOICE ? new ArrayList<>(entity.getOptions()) : List.of()) // Only send options for MC
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DomainOverviewDTO getDomainOverview(Long userId, Long domainId){
        UserDomainProgress udp = userDomainProgressRepository
                .findByUserIdAndDomainId(userId, domainId)
                .orElseThrow(() -> new NotFoundException("Start the domain first"));

        LearningPathDTO lp = parseLearningPathJson(udp.getLearningPathJson());

        int currentIdx = udp.getCurrentTopicIndex();
        TopicProgress currentTp = topicProgressRepository.findByUserDomainProgressIdAndTopicNameAndLevel(
                udp.getId(),
                lp.getTopics().get(currentIdx),
                getCurrentLevelForTopic(udp, lp.getTopics().get(currentIdx))).orElse(null);
        boolean currentReviewDone = currentTp != null && currentTp.getCompletedInsightsCount() >= currentTp.getRequiredInsightsForLevelCompletion();

        List<String> topicsList = lp.getTopics();
        List<TopicOverviewDTO> topics = new ArrayList<>();
        for(int idx=0; idx<topicsList.size(); idx++){
            String name = lp.getTopics().get(idx);
            int level = getCurrentLevelForTopic(udp, name);

            boolean current   = idx == udp.getCurrentTopicIndex();

            TopicProgress tp = topicProgressRepository
                    .findByUserDomainProgressIdAndTopicNameAndLevel(udp.getId(), name, level)
                    .orElse(null);

            boolean reviewAvailable = tp != null &&
                    tp.getCompletedInsightsCount() >= tp.getRequiredInsightsForLevelCompletion();
            boolean unlocked;
            if (level >= 2) {
                unlocked = true;
            } else if (idx == 0) {
                unlocked = true;
            } else {
                String prevTopic = topicsList.get(idx - 1);
                int prevLevel = getCurrentLevelForTopic(udp, prevTopic);
                unlocked = prevLevel >= 2;
            }
            topics.add(TopicOverviewDTO.builder()
                    .topicName(name)
                    .level(level)
                    .completedInsights(tp==null?0:tp.getCompletedInsightsCount())
                    .requiredInsights(tp==null?6:tp.getRequiredInsightsForLevelCompletion())
                    .reviewAvailable(reviewAvailable)
                    .unlocked(unlocked)
                    .current(current)
                    .build());
        }
        return DomainOverviewDTO.builder()
                .domainId(domainId)
                .domainName(lp.getDomainName())
                .topics(topics)
                .build();
    }

    @Override
    @Transactional
    public void selectTopic(Long userId, Long domainId, int topicIndex){
        UserDomainProgress udp = userDomainProgressRepository.findByUserIdAndDomainId(userId, domainId)
                .orElseThrow(() -> new NotFoundException("Progress not found"));
        LearningPathDTO lp = parseLearningPathJson(udp.getLearningPathJson());
        if(topicIndex<0 || topicIndex>=lp.getTopics().size()) throw new IllegalArgumentException("Bad topic index");

        udp.setCurrentTopicIndex(topicIndex);
        userDomainProgressRepository.save(udp);
        // ensure progress rows & first insights exist
        String currentTopicName = lp.getTopics().get(topicIndex);
        ensureTopicProgressExistsAndGenerateInsights(udp, lp.getDomainName(), lp.getTopics().get(topicIndex), getCurrentLevelForTopic(udp, currentTopicName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainStatusDTO> getDomainsWithStatus(Long userId) {
        List<Long> startedDomainIds = userDomainProgressRepository.findByUserId(userId)
                .stream()
                .map(udp -> udp.getDomain().getId())
                .collect(Collectors.toList());

        return domainRepository.findAll().stream()
                .map(domain -> {
                    DomainStatusDTO dto = new DomainStatusDTO();
                    dto.setId(domain.getId());
                    dto.setName(domain.getName());
                    dto.setDescription(domain.getDescription());
                    dto.setCategory(domain.getCategory());
                    dto.setInProgress(startedDomainIds.contains(domain.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<RichAssessmentAnswerDTO> mapAssessmentSubmissionToRichDTOs(Map<Long, String> submittedAnswers) {
        if (submittedAnswers == null || submittedAnswers.isEmpty()) {
            return Collections.emptyList();
        }
        List<RichAssessmentAnswerDTO> richAnswers = new ArrayList<>();
        for (Map.Entry<Long, String> entry : submittedAnswers.entrySet()) {
            AssessmentQuestionEntity question = assessmentQuestionRepository.findById(entry.getKey())
                    .orElseThrow(() -> new NotFoundException("Assessment question not found: " + entry.getKey()));
            richAnswers.add(RichAssessmentAnswerDTO.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .options(new ArrayList<>(question.getOptions())) // Defensive copy
                    .selectedAnswer(entry.getValue())
                    .build());
        }
        return richAnswers;
    }

    @Override
    @Transactional(readOnly = true)
    public int countCompletedInsights(Long userId) {
        // Sum the completed insight counters on every TopicProgress
        return userDomainProgressRepository.findByUserId(userId)
                .stream()
                .flatMap(udp -> udp.getTopicProgresses().stream())
                .mapToInt(TopicProgress::getCompletedInsightsCount)
                .sum();
    }
}