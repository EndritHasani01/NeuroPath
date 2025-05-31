package com.example.adaptivelearningbackend.controller;

import com.example.adaptivelearningbackend.dto.DomainStatusDTO;
import com.example.adaptivelearningbackend.dto.RegisterRequestDTO;
import com.example.adaptivelearningbackend.dto.UserDTO;
import com.example.adaptivelearningbackend.dto.UserProfileDTO;
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

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:3000") // Handled globally
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final LearningService learningService;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        try {
            UserDTO newUser = userService.registerUser(registerRequest);
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("User registration failed for username {}: {}", registerRequest.getUsername(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication auth) {
        CustomUserDetails cd = (CustomUserDetails) auth.getPrincipal();
        Long userId = cd.getId();

        UserDTO user = userService.getUserById(userId);
        List<DomainStatusDTO> status = learningService.getDomainsWithStatus(userId);

        long completedDomains = status.stream().filter(ds -> !ds.isInProgress()).count();
        double overall = status.isEmpty() ? 0 : (completedDomains * 100.0 / status.size());

        int startedDomains = (int) status.stream()
                .filter(DomainStatusDTO::isInProgress)
                .count();

        int completedInsights = learningService.countCompletedInsights(userId);

        UserProfileDTO profile = UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .overallProgress(overall)
                .domains(status)
                .startedDomains(startedDomains)
                .completedInsights(completedInsights)
                .build();

        return ResponseEntity.ok(profile);
    }


}