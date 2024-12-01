package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    @Query(value = "{ 'email': ?0 }", exists = true)
    boolean existsByEmail(String email);

    @Query(value = "{ 'username': ?0 }", exists = true)
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("{ 'deletedAt': null }")
    List<User> findAllActive();
}
