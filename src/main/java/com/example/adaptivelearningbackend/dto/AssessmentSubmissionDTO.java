package com.example.adaptivelearningbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class AssessmentSubmissionDTO {
    @NotNull
    private Long domainId;
    @NotNull
    private Map<Long, String> answers; // Key: assessmentQuestionId, Value: selected answer/option
}