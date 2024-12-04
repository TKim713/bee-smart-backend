package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TopicRepository extends MongoRepository<Topic, String> {
    Page<Topic> findByGrade_GradeIdAndSemesterAndDeletedAtIsNull(String gradeId, String semester, Pageable pageable);
}

