package com.example.adaptivelearningbackend.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String username;   // or email
    private String password;
}
