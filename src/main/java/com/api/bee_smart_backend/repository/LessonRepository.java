package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Lesson;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface LessonRepository extends MongoRepository<Lesson, String> {
    Page<Lesson> findByTopic(Topic topic, Pageable pageable);

    Page<Lesson> findByLessonNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String lessonName, String description, Pageable pageable);

    @Query(value = "{ 'topic' : ?0 }", sort = "{ '_id' : 1 }")
    Lesson findFirstByTopicOrderByIdAsc(Topic topic);

    List<Lesson> findByTopic(Topic topic);

    @Query("{ 'topic' : ?0, '$or' : [ { 'lessonName' : { '$regex' : ?1, '$options' : 'i' } }, { 'description' : { '$regex' : ?1, '$options' : 'i' } } ] }")
    Page<Lesson> findByTopicAndSearch(Topic topic, String search, Pageable pageable);
}

