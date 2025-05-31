package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.UserDomainProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserDomainProgressRepository extends JpaRepository<UserDomainProgress, Long> {
    Optional<UserDomainProgress> findByUserIdAndDomainId(Long userId, Long domainId);
    List<UserDomainProgress> findByUserId(Long userId);
}