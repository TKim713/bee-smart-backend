package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.view.LessonView;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LessonViewRepository extends MongoRepository<LessonView, String> {

    List<LessonView> findAllByCreatedAtBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    Optional<LessonView> findByLessonIdAndCreatedAt(String lessonId, LocalDate today);
}
