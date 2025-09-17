package com.example.adaptivelearningbackend.service.impl;

import com.example.adaptivelearningbackend.dto.JwtResponseDTO;
import com.example.adaptivelearningbackend.dto.LoginRequestDTO;
import com.example.adaptivelearningbackend.dto.RegisterRequestDTO;
import com.example.adaptivelearningbackend.dto.UserDTO;
import com.example.adaptivelearningbackend.entity.RoleEntity;
import com.example.adaptivelearningbackend.entity.UserEntity;
import com.example.adaptivelearningbackend.exception.NotFoundException;
import com.example.adaptivelearningbackend.exception.UserAlreadyExistsException;
import com.example.adaptivelearningbackend.repository.RoleRepository;
import com.example.adaptivelearningbackend.repository.UserRepository;
import com.example.adaptivelearningbackend.security.JwtTokenProvider;
import com.example.adaptivelearningbackend.service.UserService;
import lombok.RequiredArgsConstructor;
// import org.springframework.security.crypto.password.PasswordEncoder; // Uncomment when proper security is added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
     private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public JwtResponseDTO login(LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            UserDetails principal = (UserDetails) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(principal);
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            logger.info("User {} authenticated successfully", principal.getUsername());
            return new JwtResponseDTO(token, principal.getUsername(), roles);
        } catch (AuthenticationException ex) {
            logger.warn("Failed login attempt for {}: {}", request.getUsername(), ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", ex);
        }
    }
    @Override
    @Transactional
    public UserDTO registerUser(RegisterRequestDTO registerRequest) {
        validateRegisterRequest(registerRequest);
        ensureUniqueCredentials(registerRequest);

        RoleEntity defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new NotFoundException("Default role not configured: " + DEFAULT_ROLE));

        UserEntity user = UserEntity.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .roles(new HashSet<>(Set.of(defaultRole)))
                .build();

        UserEntity savedUser = userRepository.save(user);
        logger.info("Created user {} with id {}", savedUser.getUsername(), savedUser.getId());
        return mapToUserDTO(savedUser);
    }

    @Override
    public UserDTO getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return mapToUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsernameOrEmail(String usernameOrEmail) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));

        UserEntity user = userOpt
                .orElseThrow(() -> new NotFoundException("User not found with username or email: " + usernameOrEmail));
        return mapToUserDTO(user);
    }

    private void validateRegisterRequest(RegisterRequestDTO registerRequest) {
        Objects.requireNonNull(registerRequest, "Register request must not be null");

        if (!Objects.equals(registerRequest.getPassword(), registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    private void ensureUniqueCredentials(RegisterRequestDTO registerRequest) {
        userRepository.findByUsername(registerRequest.getUsername()).ifPresent(user -> {
            throw new UserAlreadyExistsException("Username already exists: " + registerRequest.getUsername());
        });

        userRepository.findByEmail(registerRequest.getEmail()).ifPresent(user -> {
            throw new UserAlreadyExistsException("Email already exists: " + registerRequest.getEmail());
        });
    }

    private UserDTO mapToUserDTO(UserEntity userEntity) {
        UserDTO dto = new UserDTO();
        dto.setId(userEntity.getId());
        dto.setUsername(userEntity.getUsername());
        dto.setEmail(userEntity.getEmail());
        dto.setCreatedAt(userEntity.getCreatedAt());
        return dto;
    }
}