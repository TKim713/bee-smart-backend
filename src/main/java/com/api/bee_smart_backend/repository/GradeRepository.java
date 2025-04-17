package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface GradeRepository extends MongoRepository<Grade, String> {
    @Query("{ 'gradeName' : ?0 }")
    Optional<Grade> findByGradeNameAndDeletedAtIsNull(String gradeName);

    Page<Grade> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Grade> findByGradeNameContainingIgnoreCaseAndDeletedAtIsNull(String search, Pageable pageable);
}

