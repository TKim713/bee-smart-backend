package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    @Query(value = "SELECT topic_id FROM Topic t WHERE t.topic_name = :topicName", nativeQuery = true)
    Optional<Topic> findByTopicName(String topicName);
}
