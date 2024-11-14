package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    @Query(value = "SELECT * FROM topic t WHERE t.topic_name = :topicName", nativeQuery = true)
    Optional<Topic> findByTopicName(String topicName);

    @Query(value = "SELECT * FROM topic ORDER BY topic_id ASC LIMIT 1;\n", nativeQuery = true)
    Topic findFirstByOrderByIdAsc();

    List<Topic> findByChapter(Chapter chapter);

    long countByChapter(Chapter chapter);
}
