package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubjectRepository extends MongoRepository<Subject, String> {
    Optional<Subject> findBySubjectNameAndDeletedAtIsNull(String subject);

    Page<Subject> findBySubjectNameContainingIgnoreCaseAndDeletedAtIsNull(String search, Pageable pageable);

    Page<Subject> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Subject> findBySubjectIdAndDeletedAtIsNull(String subjectId);

    Subject findBySubjectName(String subjectName);
}
