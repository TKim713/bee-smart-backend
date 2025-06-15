package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.model.record.QuizRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface QuizRecordRepository extends MongoRepository<QuizRecord, String> {

    Page<QuizRecord> findByQuizContainingIgnoreCaseOrUserContainingIgnoreCase(String search, String search1, Pageable pageable);

    List<QuizRecord> findAllBySubmitDateBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    Page<QuizRecord> findByUser(User user, Pageable pageable);

    @Query("{ 'user' : ?0, 'quiz.title' : { '$regex' : ?1, '$options' : 'i' } }")
    Page<QuizRecord> findByUserAndQuizTitleContainingIgnoreCase(User user, String search, Pageable pageable);

    List<QuizRecord> findByUserAndSubmitDateBetween(User user, LocalDateTime start, LocalDateTime end);

    Page<QuizRecord> findByUserAndDeletedAtIsNull(User user, Pageable pageable);

    List<QuizRecord> findByQuizAndDeletedAtIsNull(Quiz quiz);

    @Query("{ 'user' : ?0, 'quiz.title' : { '$regex' : ?1, '$options' : 'i' } }")
    Page<QuizRecord> findByUserAndQuizTitleContainingIgnoreCaseAndDeletedAtIsNull(User user, String search, Pageable pageable);
}
