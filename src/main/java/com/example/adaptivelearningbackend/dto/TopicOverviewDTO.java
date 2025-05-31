package com.example.adaptivelearningbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicOverviewDTO {
    private String topicName;
    private int level;                       // current level in this topic
    private int completedInsights;           // finished in this level
    private int requiredInsights;            // usually 6
    private boolean reviewAvailable;         // enable Review btn
    private boolean unlocked;                // enable Learn btn
    private boolean current;                 // learner’s active topic
}
