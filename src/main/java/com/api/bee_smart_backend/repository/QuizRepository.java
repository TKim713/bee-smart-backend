package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Arrays;
import java.util.List;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    Page<Quiz> findByLessonInAndDeletedAtIsNull(List<Lesson> lessons, Pageable pageable);

    Page<Quiz> findByLessonInAndTitleContainingIgnoreCaseAndDeletedAtIsNull(List<Lesson> lessons, String title, Pageable pageable);

    Page<Quiz> findByLessonAndDeletedAtIsNull(Lesson lesson, Pageable pageable);

    Page<Quiz> findByTopicAndDeletedAtIsNull(Topic topic, Pageable pageable);

    @Query("{ 'topic' : ?0, '$or' : [ { 'topicName' : { '$regex' : ?1, '$options' : 'i' } }] }")
    Page<Quiz> findByTopicAndSearchAndDeletedAtIsNull(Topic topic, String search, Pageable pageable);
}
