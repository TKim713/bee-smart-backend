package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.helper.response.LessonResponse;
import com.api.bee_smart_backend.model.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    //List<LessonResponse> getListLesson();

    @Query(value = "SELECT l FROM Lesson l WHERE LOWER(l.lesson_name) LIKE LOWER(CONCAT('%', :search, '%'))", nativeQuery = true)
    Page<Lesson> findByNameContainingIgnoreCase(String search, Pageable pageable);
}
