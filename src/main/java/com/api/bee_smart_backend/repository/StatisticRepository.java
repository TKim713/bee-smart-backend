package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Statistic;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StatisticRepository extends MongoRepository<Statistic, String> {
    Optional<Statistic> findByUserAndDeletedAtIsNull(User user);
}
