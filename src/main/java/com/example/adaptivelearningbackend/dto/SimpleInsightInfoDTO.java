package com.example.adaptivelearningbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleInsightInfoDTO {
    private Long insightId;
    private String title; // Might be useful for LlamaIndex
    private double relevanceScore; // From DB
    private int timesShown; // From DB
    // Add any other minimal data LlamaIndex might need for selection from the existing list
}