package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Parent;
import com.api.bee_smart_backend.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StudentRepository extends MongoRepository<Student, String> {
    List<Student> findByParent(Parent parent);
}
