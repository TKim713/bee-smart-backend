package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
}
