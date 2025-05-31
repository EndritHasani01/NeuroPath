package com.example.adaptivelearningbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity @Table(name="roles")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=32)
    private String name;              // e.g. ROLE_USER, ROLE_ADMIN
}
