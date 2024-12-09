package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Parent;
import com.api.bee_smart_backend.model.Student;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    List<Student> findByParent(Parent parent);

    Optional<Student> findByUserAndDeletedAtIsNull(User user);
}
