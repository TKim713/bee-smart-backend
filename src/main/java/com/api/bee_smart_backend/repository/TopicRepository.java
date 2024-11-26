package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends MongoRepository<Topic, String> {
    @Query("{ 'topicName' : ?0 }")
    Optional<Topic> findByTopicName(String topicName);

    @Query(value = "{}", sort = "{ '_id' : 1 }")
    Topic findFirstByOrderByIdAsc();

    Page<Topic> findByChapter(Chapter chapter, Pageable pageable);

    long countByChapterIn(List<Chapter> chapters);
}

