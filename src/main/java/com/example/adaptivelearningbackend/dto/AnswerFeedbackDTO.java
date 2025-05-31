package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFeedbackDTO {
    private Long questionId;
    private String selectedAnswer;
    private boolean correct;
    private String correctAnswer;
    private String feedback;
}