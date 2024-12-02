package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    Page<Quiz> findByLessonIn(List<Lesson> lessons, Pageable pageable);

    Page<Quiz> findByLessonInAndTitleContainingIgnoreCase(List<Lesson> lessons, String title, Pageable pageable);

    Page<Quiz> findByLesson(Lesson lesson, Pageable pageable);
}
