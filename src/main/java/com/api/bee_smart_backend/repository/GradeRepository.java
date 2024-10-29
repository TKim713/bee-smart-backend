package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    @Query(value = "SELECT grade_id FROM Grade g WHERE g.grade_name = :gradeName", nativeQuery = true)
    Optional<Grade> findByGradeName(String gradeName);
}
