package com.example.adaptivelearningbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmissionDTO {
    /*@NotNull*/
    private Long questionId;
    @NotNull
    private String selectedAnswer;
    private Long timeTakenMs;
    private boolean forReview;
}