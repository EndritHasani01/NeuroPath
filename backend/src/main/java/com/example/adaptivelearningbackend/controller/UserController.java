package com.example.adaptivelearningbackend.controller;

import com.example.adaptivelearningbackend.dto.*;
import com.example.adaptivelearningbackend.security.CustomUserDetails;
import com.example.adaptivelearningbackend.service.LearningService;
import com.example.adaptivelearningbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:3000") // Handled globally
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    public JwtResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return userService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        UserDTO createdUser = userService.registerUser(request);
        URI location = URI.create("/api/users/" + createdUser.getId());
        return ResponseEntity.created(location).body(createdUser);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication auth) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        return ResponseEntity.ok(userService.getProfileForUser(userId));
    }

}