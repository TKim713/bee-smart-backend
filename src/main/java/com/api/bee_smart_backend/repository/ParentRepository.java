package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Parent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParentRepository extends MongoRepository<Parent, String> {
}
