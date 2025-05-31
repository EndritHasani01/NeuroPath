package com.example.adaptivelearningbackend.controller;

import com.example.adaptivelearningbackend.dto.*;
import com.example.adaptivelearningbackend.entity.*;
import com.example.adaptivelearningbackend.repository.RoleRepository;
import com.example.adaptivelearningbackend.repository.UserRepository;
import com.example.adaptivelearningbackend.security.JwtTokenProvider;
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

    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwt;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @PostMapping("/login")
    public JwtResponseDTO login(@RequestBody LoginRequestDTO req) {
        Authentication authentication =
                authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        UserDetails user = (UserDetails) authentication.getPrincipal();
        String token = jwt.generateToken(user);
        return new JwtResponseDTO(token, user.getUsername(),
                authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO req) {
        if (users.findByUsername(req.getUsername()).isPresent()) return ResponseEntity.badRequest().body("Username taken");
        if (!req.getPassword().equals(req.getConfirmPassword())) return ResponseEntity.badRequest().body("Passwords do not match");
        RoleEntity userRole = roles.findByName("ROLE_USER").orElseThrow();
        UserEntity u = UserEntity.builder()
                .username(req.getUsername())
                .email(req.getUsername())       // simple demo – expect email as username
                .password(encoder.encode(req.getPassword()))
                .roles(Set.of(userRole))
                .build();
        users.save(u);
        return ResponseEntity.created(URI.create("/api/users/"+u.getId())).build();
    }
}
