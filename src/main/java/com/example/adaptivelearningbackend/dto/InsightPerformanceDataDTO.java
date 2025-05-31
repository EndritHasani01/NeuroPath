package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Matches Python's InsightPerformanceData
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightPerformanceDataDTO {
    private Long insightId;
    private String insightTitle;
    private List<UserAnswerDetailDTO> questionsAnswered;
    private Integer timesShown;
}
