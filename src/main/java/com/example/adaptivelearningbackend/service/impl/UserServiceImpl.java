package com.example.adaptivelearningbackend.service.impl;

import com.example.adaptivelearningbackend.dto.RegisterRequestDTO;
import com.example.adaptivelearningbackend.dto.UserDTO;
import com.example.adaptivelearningbackend.entity.UserEntity;
import com.example.adaptivelearningbackend.exception.NotFoundException;
import com.example.adaptivelearningbackend.exception.UserAlreadyExistsException;
import com.example.adaptivelearningbackend.repository.UserRepository;
import com.example.adaptivelearningbackend.service.UserService;
import lombok.RequiredArgsConstructor;
// import org.springframework.security.crypto.password.PasswordEncoder; // Uncomment when proper security is added
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
     private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final ModelMapper mapper;

    @Override
    @Transactional
    public UserDTO registerUser(RegisterRequestDTO registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists: " + registerRequest.getUsername());
        }
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists: " + registerRequest.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword())) // Hash password
                .build();

        UserEntity savedUser = userRepository.save(user);
        return mapToUserDTO(savedUser);
    }

    @Override
    public UserDTO getUserById(Long id) {
        UserEntity u = userRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return mapper.map(u, UserDTO.class);
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