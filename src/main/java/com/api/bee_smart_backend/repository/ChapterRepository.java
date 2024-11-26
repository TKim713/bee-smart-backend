package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Grade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends MongoRepository<Chapter, String> {
    List<Chapter> findByGrade(Grade grade);

    @Query("{ 'chapterName' : ?0 }")
    Optional<Chapter> findByChapterName(String chapterName);
}

