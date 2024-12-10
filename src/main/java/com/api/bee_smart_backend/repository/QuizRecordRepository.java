package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.record.QuizRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface QuizRecordRepository extends MongoRepository<QuizRecord, String> {

    Page<QuizRecord> findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(String search, String search1, Pageable pageable);

    List<QuizRecord> findAllBySubmitDateBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);
}
