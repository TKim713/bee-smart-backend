package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TopicRepository extends MongoRepository<Topic, String> {
    Page<Topic> findBySubject_SubjectIdAndGrade_GradeIdAndSemesterAndDeletedAtIsNull(String subjectId, String gradeId, String semester, Pageable pageable);

    Page<Topic> findBySubject_SubjectIdAndGrade_GradeIdAndSemesterAndBookType_BookIdAndDeletedAtIsNull(String subjectId, String gradeId, String semester, String bookId, Pageable pageable);
}

