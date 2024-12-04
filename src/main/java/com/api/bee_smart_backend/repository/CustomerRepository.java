package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Customer;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByUserAndDeletedAtIsNull(User user);
}
