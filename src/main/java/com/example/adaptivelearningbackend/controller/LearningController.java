package com.example.adaptivelearningbackend.controller;

import com.example.adaptivelearningbackend.dto.*;
import com.example.adaptivelearningbackend.entity.UserEntity;
import com.example.adaptivelearningbackend.repository.UserRepository;
import com.example.adaptivelearningbackend.security.CustomUserDetails;
import com.example.adaptivelearningbackend.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:3000") // Handled globally now in SecurityConfig
public class LearningController {

    private static final Logger logger = LoggerFactory.getLogger(LearningController.class);
    private final LearningService learningService;
    private final UserRepository userRepository;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else {
            // Fallback: load by username
            String username = auth.getName();
            UserEntity u = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            return u.getId();
        }
    }


    @GetMapping("/domains")
    public ResponseEntity<List<DomainDTO>> getDomains() {
        return ResponseEntity.ok(learningService.getAllDomains());
    }

    @GetMapping("/domains/{domainId}/assessment-questions")
    public ResponseEntity<List<AssessmentQuestionDTO>> getAssessmentQuestions(@PathVariable Long domainId) {
        try {
            return ResponseEntity.ok(learningService.getAssessmentQuestions(domainId));
        } catch (Exception e) { // More specific exceptions can be caught
            logger.error("Error fetching assessment questions for domain {}", domainId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/domains/start")
    public ResponseEntity<LearningPathDTO> startDomain(@Valid @RequestBody AssessmentSubmissionDTO submission) {
        Long userId = getCurrentUserId(); // Placeholder
        try {
            LearningPathDTO path = learningService.startDomainAndGetLearningPath(userId, submission);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            logger.error("Error starting domain {} for user {}", submission.getDomainId(), userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to start domain: " + e.getMessage(), e);
        }
    }

    @GetMapping("/domains/{domainId}/next-insight")
    public ResponseEntity<InsightDTO> getNextInsight(@PathVariable Long domainId) {
        Long userId = getCurrentUserId(); // Placeholder
        try {
            InsightDTO insight = learningService.getNextInsight(userId, domainId);
            if (insight == null) {
                return ResponseEntity.noContent().build(); // Or a specific DTO indicating review is ready
            }
            return ResponseEntity.ok(insight);
        } catch (Exception e) {
            logger.error("Error getting next insight for user {} domain {}", userId, domainId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not retrieve next insight: " + e.getMessage(), e);
        }
    }

    @PostMapping("/insights/submit-answer")
    public ResponseEntity<AnswerFeedbackDTO> submitAnswer(@Valid @RequestBody AnswerSubmissionDTO submission) {
        Long userId = getCurrentUserId(); // Placeholder
        try {
            return ResponseEntity.ok(learningService.submitAnswer(userId, submission));
        } catch (Exception e) {
            logger.error("Error submitting answer for user {}, question {}", userId, submission.getQuestionId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to submit answer: " + e.getMessage(), e);
        }
    }

    @GetMapping("/domains/{domainId}/progress")
    public ResponseEntity<TopicProgressDTO> getTopicProgress(@PathVariable Long domainId) {
        Long userId = getCurrentUserId(); // Placeholder
        try {
            return ResponseEntity.ok(learningService.getTopicProgress(userId, domainId));
        } catch (Exception e) {
            logger.error("Error fetching topic progress for user {} domain {}", userId, domainId, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not retrieve topic progress: " + e.getMessage(), e);
        }
    }

    @GetMapping("/domains/{domainId}/review")
    public ResponseEntity<ReviewDTO> getReview(@PathVariable Long domainId) {
        Long userId = getCurrentUserId(); // Placeholder
        try {
            return ResponseEntity.ok(learningService.getReview(userId, domainId));
        } catch (IllegalStateException e) {
            logger.warn("Review requested prematurely for user {} domain {}: {}", userId, domainId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error generating review for user {} domain {}", userId, domainId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate review: " + e.getMessage(), e);
        }
    }

    @PostMapping("/domains/{domainId}/complete-review")
    public ResponseEntity<Void> completeReview(@PathVariable Long domainId, @RequestParam boolean satisfactoryPerformance) {
        Long userId = getCurrentUserId(); // Placeholder
        try {
            learningService.completeReviewAndAdvance(userId, domainId, satisfactoryPerformance);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error completing review for user {} domain {}: {}", userId, domainId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to complete review: " + e.getMessage(), e);
        }
    }

    @GetMapping("/domains/{domainId}/overview")
    public ResponseEntity<DomainOverviewDTO> overview(@PathVariable Long domainId){
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(learningService.getDomainOverview(userId, domainId));
    }

    @PostMapping("/domains/{domainId}/select-topic/{topicIdx}")
    public ResponseEntity<Void> select(@PathVariable Long domainId, @PathVariable int topicIdx){
        Long userId = getCurrentUserId();
        learningService.selectTopic(userId, domainId, topicIdx);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/domains/status")
    public ResponseEntity<List<DomainStatusDTO>> getDomainsWithStatus() {
        Long userId = getCurrentUserId(); // from security context eventually
        return ResponseEntity.ok(learningService.getDomainsWithStatus(userId));
    }
}