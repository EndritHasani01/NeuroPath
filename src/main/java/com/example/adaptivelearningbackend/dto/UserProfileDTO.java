package com.example.adaptivelearningbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfileDTO {
    private Long id;
    private String username;
    private String email;
    private double overallProgress;           // 0–100
    private List<DomainStatusDTO> domains;   // from LearningService.getDomainsWithStatus
    private int startedDomains;
    private int completedInsights;
}
