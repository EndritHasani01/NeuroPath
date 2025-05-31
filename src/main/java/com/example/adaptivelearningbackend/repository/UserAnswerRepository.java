package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.UserAnswer;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    List<UserAnswer> findByUserIdAndQuestionInsightId(Long userId, Long insightId);
    List<UserAnswer> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserAnswer ua WHERE ua.question.insight.id = :insightId")
    void deleteByQuestionInsightId(@Param("insightId") Long insightId);
}