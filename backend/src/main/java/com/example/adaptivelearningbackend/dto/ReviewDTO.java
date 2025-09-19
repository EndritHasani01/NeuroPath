package com.example.adaptivelearningbackend.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ReviewDTO {
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<QuestionDTO> revisionQuestions;
}