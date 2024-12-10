package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.record.LessonRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LessonRecordRepository extends MongoRepository<LessonRecord, String> {

    List<LessonRecord> findAllByCreatedAtBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    Optional<LessonRecord> findByLessonIdAndCreatedAt(String lessonId, LocalDate today);
}
