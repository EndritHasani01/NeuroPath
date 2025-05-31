package com.example.adaptivelearningbackend.dto;

import com.example.adaptivelearningbackend.enums.QuestionType;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class QuestionDTO {
    private Long id;
    private QuestionType questionType;
    private String questionText;
    private List<String> options; // Null or empty for TRUE_FALSE if not needed explicitly
}