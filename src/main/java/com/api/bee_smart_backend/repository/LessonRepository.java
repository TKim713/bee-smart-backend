package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Page<Lesson> findByTopic(Topic topic, Pageable pageable);

    long countByTopic(Topic topic);

    @Query(value = "SELECT l FROM Lesson l WHERE LOWER(l.lesson_name) LIKE LOWER(CONCAT('%', :search, '%'))", nativeQuery = true)
    Page<Lesson> findByNameContainingIgnoreCase(String search, Pageable pageable);

    @Query(value = "SELECT * FROM lesson WHERE topic_id = :topicId ORDER BY id ASC LIMIT 1;\n", nativeQuery = true)
    Lesson findFirstByTopicOrderByIdAsc(Topic topic);

    List<Lesson> findByTopic(Topic topic);
}
