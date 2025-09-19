package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.InsightEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsightRepository extends JpaRepository<InsightEntity, Long> {
    List<InsightEntity> findByTopicProgressId(Long topicProgressId);
    List<InsightEntity> findByTopicProgressIdAndLevel(Long topicProgressId, int level);


    @Query("""
            SELECT i 
            FROM InsightEntity i 
            WHERE i.topicProgress.id = :topicProgressId 
            AND i.completed = false 
            ORDER BY i.lastAccessedAt ASC, i.relevanceScore DESC
            """)
    List<InsightEntity> findUncompletedInsightsForTopic(@Param("topicProgressId") Long topicProgressId);

    long countByTopicProgressId(Long topicProgressId);
}