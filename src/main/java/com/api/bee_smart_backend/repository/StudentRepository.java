package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudentRepository extends MongoRepository<Student, String> {
}
