package com.example.adaptivelearningbackend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssessmentQuestionDTO {
    private Long id;
    private String questionText;
    private List<String> options;
}