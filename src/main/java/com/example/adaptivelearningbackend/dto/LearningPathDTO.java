package com.example.adaptivelearningbackend.dto;

import lombok.Data;
import java.util.List;

@Data
public class LearningPathDTO {
    private String domainName;
    private List<String> topics; // Ordered list of topic names
}