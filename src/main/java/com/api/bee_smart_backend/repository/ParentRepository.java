package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Parent;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ParentRepository extends MongoRepository<Parent, String> {
    Optional<Parent> findByUserAndDeletedAtIsNull(User user);
}
