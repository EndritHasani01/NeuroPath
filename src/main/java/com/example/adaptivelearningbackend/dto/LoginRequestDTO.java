package com.example.adaptivelearningbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank(message = "Username cannot be blank")
    private String username;   // or email
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
