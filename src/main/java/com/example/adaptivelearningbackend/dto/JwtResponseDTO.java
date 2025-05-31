package com.example.adaptivelearningbackend.dto;

import lombok.*;

import java.util.Set;

@Getter @Setter @AllArgsConstructor
public class JwtResponseDTO {
    private String token;
    private String username;
    private Set<String> roles;
}
