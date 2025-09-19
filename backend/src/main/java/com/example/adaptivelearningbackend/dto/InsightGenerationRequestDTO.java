package com.example.adaptivelearningbackend.dto;

import com.example.adaptivelearningbackend.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightGenerationRequestDTO {
    private Long userId;
    private String domain;
    private String topic;
    private int level;
    private Map<String, Object> userPerformanceMetrics; // e.g. overall accuracy, common mistakes

    // This DTO represents what the AI is expected to return for each insight
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightDetailDTO {
        private String title;
        private String explanation;
        private Map<String, Object> aiMetadata; // Any extra data from AI
        private List<QuestionDetailDTO> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetailDTO {
        private QuestionType questionType;
        private String questionText;
        private List<String> options; // For MULTIPLE_CHOICE
        private String correctAnswer; // "true", "false" or option text/index
        private Map<String, String> answerFeedbacks; // Key: option/answer, Value: feedback text
    }
}