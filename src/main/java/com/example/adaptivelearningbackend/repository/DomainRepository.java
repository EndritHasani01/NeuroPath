package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.DomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DomainRepository extends JpaRepository<DomainEntity, Long> {
    Optional<DomainEntity> findByName(String name);
}