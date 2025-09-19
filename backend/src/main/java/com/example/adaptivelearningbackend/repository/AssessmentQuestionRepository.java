package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.AssessmentQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestionEntity, Long> {
    List<AssessmentQuestionEntity> findByDomainId(Long domainId);
}