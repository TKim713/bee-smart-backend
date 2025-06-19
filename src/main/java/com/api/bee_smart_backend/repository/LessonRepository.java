package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface LessonRepository extends MongoRepository<Lesson, String> {
    Page<Lesson> findByTopicAndDeletedAtIsNull(Topic topic, Pageable pageable);

    Page<Lesson> findByLessonNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndDeletedAtIsNull(String lessonName, String description, Pageable pageable);

    List<Lesson> findByTopicAndDeletedAtIsNull(Topic topic);

    @Query("{ 'topic' : ?0, '$or' : [ { 'lessonName' : { '$regex' : ?1, '$options' : 'i' } }, { 'description' : { '$regex' : ?1, '$options' : 'i' } } ] }")
    Page<Lesson> findByTopicAndSearchAndDeletedAtIsNull(Topic topic, String search, Pageable pageable);

    Page<Lesson> findByDeletedAtIsNull(Pageable pageable);
}

