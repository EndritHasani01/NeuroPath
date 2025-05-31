package com.example.adaptivelearningbackend.dto;

import lombok.Data;
import java.util.List;

@Data
public class DomainDTO {
    private Long id;
    private String name;
    private String description;

    private String category;
}