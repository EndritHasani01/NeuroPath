package com.example.adaptivelearningbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    @NotBlank
    private String username;
    @NotBlank @Email
    private String email;
    private LocalDateTime createdAt;
}