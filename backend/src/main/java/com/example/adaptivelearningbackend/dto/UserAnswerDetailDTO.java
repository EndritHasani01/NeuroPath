package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Matches Python's UserAnswerDetail
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswerDetailDTO {
    private Long questionId;
    private String questionText;
    private List<String> options;
    private String selectedAnswer;
    private String correctAnswer;
    private boolean isCorrect;
    private Long timeTakenMs;
}
