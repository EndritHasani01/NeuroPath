package com.example.adaptivelearningbackend.repository;

import com.example.adaptivelearningbackend.entity.TopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {
    Optional<TopicProgress> findByUserDomainProgressIdAndTopicNameAndLevel(Long userDomainProgressId, String topicName, int level);
    List<TopicProgress> findByUserDomainProgressIdOrderByTopicNameAscLevelAsc(Long userDomainProgressId);
}