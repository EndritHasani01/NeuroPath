package com.example.adaptivelearningbackend.dto;

import com.example.adaptivelearningbackend.enums.QuestionType;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InsightDTO {
    private Long id;
    private String title;
    private String explanation;
    private Map<String, Object> aiMetadata; // Flexible for AI needs
    private boolean completed;
    private List<QuestionDTO> questions;
}