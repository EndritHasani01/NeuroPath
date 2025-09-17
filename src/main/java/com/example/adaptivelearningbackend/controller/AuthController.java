package com.example.adaptivelearningbackend.controller;

import com.example.adaptivelearningbackend.dto.*;
import com.example.adaptivelearningbackend.entity.*;
import com.example.adaptivelearningbackend.repository.RoleRepository;
import com.example.adaptivelearningbackend.repository.UserRepository;
import com.example.adaptivelearningbackend.security.JwtTokenProvider;
import com.example.adaptivelearningbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService authService;

//    private final AuthenticationManager authManager;
//    private final JwtTokenProvider jwt;
//    private final UserRepository users;
//    private final RoleRepository roles;
//    private final PasswordEncoder encoder;

    @PostMapping("/login")
    public JwtResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        UserDTO createdUser = authService.registerUser(request);
        URI location = URI.create("/api/users/" + createdUser.getId());
        return ResponseEntity.created(location).body(createdUser);
    }
}
