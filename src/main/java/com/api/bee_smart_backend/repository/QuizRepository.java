package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Quiz;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface QuizRepository extends MongoRepository<Quiz, String> {
    Page<Quiz> findByTopicAndLessonIsNullAndDeletedAtIsNull(Topic topic, Pageable pageable);

    @Query("{ 'topic' : ?0, 'lesson': null, 'deletedAt': null, '$or' : [ { 'topicName' : { '$regex' : ?1, '$options' : 'i' } }] }")
    Page<Quiz> findByTopicAndLessonIsNullAndSearchAndDeletedAtIsNull(Topic topic, String search, Pageable pageable);

    Page<Quiz> findByLessonAndDeletedAtIsNull(Lesson lesson, Pageable pageable);
}
