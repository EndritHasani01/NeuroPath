package com.example.adaptivelearningbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DomainOverviewDTO {
    private Long domainId;
    private String domainName;
    private List<TopicOverviewDTO> topics;   // ordered as in learning‑path
}