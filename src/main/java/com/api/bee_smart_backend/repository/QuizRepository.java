package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Quiz;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuizRepository extends MongoRepository<Quiz, String> {
}
