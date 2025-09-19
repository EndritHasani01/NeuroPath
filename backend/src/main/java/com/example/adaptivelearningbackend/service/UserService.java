package com.example.adaptivelearningbackend.service;

import com.example.adaptivelearningbackend.dto.*;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    JwtResponseDTO login(LoginRequestDTO request);

    UserDTO registerUser(RegisterRequestDTO registerRequest);

    @Transactional(readOnly = true)
    UserProfileDTO getProfileForUser(Long userId);

    UserDTO getUserById(Long id);
    UserDTO getUserByUsernameOrEmail(String usernameOrEmail);
    // Add other methods like:
    // UserDTO loginUser(LoginRequestDTO loginRequest);
    // UserDTO findUserById(Long id);
    // UserDTO getCurrentUser(); // Important for getting authenticated user context
}