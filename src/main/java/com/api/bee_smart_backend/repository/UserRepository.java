package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUserIdAndDeletedAtIsNull(String userId);
}
