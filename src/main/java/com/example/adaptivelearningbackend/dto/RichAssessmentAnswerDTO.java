package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Matches Python's AssessmentAnswer
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RichAssessmentAnswerDTO { // Renamed to avoid conflict if AssessmentAnswerDTO exists
    private Long questionId;
    private String questionText;
    private List<String> options;
    private String selectedAnswer;
}
