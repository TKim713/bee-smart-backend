package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Battle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BattleRepository extends MongoRepository<Battle, String> {
    Page<Battle> findByTopicContainingIgnoreCase(String search, Pageable pageable);

    List<Battle> findAllByStatus(String ongoing);
}
