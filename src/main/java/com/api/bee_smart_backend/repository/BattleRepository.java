package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Battle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface BattleRepository extends MongoRepository<Battle, String> {
    Page<Battle> findByTopicContainingIgnoreCase(String search, Pageable pageable);

    List<Battle> findAllByStatus(String ongoing);

    @Query("{ 'startTime': { $gte: ?0, $lte: ?1 } }")
    List<Battle> findAllByStartTimeBetween(LocalDate startDate, LocalDate endDate);

    Page<Battle> findByPlayerScoresUserIdAndStatus(String userId, String ended, Pageable pageable);
}
