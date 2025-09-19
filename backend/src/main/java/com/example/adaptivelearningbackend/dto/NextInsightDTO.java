package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextInsightDTO {
    private Long insightId; // The ID of the chosen insight from the DB
    // The Java backend will then fetch the full InsightDTO using this ID
}