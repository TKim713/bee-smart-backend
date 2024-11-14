package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Chapter;
import com.api.bee_smart_backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByGrade(Grade grade);

    @Query(value = "SELECT * FROM Chapter c WHERE c.chapter_name = :chapterName", nativeQuery = true)
    Optional<Chapter> findByChapterName(String chapterName);
}
