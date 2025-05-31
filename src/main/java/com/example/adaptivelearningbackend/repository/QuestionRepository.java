package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.QuestionEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findByInsightId(Long insightId);

    @Modifying
    @Transactional
    @Query("DELETE FROM QuestionEntity q WHERE q.insight.id = :insightId")
    void deleteByInsightId(@Param("insightId") Long insightId);
}