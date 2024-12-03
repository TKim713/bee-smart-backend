package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {
}
