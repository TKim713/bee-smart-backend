package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends MongoRepository<Subject, String> {
    Optional<Subject> findBySubjectNameAndDeletedAtIsNull(String subject);

    List<Subject> findAllByDeletedAtIsNull();
}
