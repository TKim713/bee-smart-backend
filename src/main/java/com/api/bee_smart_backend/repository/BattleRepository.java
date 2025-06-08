package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Battle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BattleRepository extends MongoRepository<Battle, String> {
    Page<Battle> findByTopicContainingIgnoreCase(String search, Pageable pageable);

    List<Battle> findAllByStatus(String ongoing);

    @Query("{ 'startTime': { $gte: ?0, $lte: ?1 } }")
    List<Battle> findAllByStartTimeBetween(LocalDate startDate, LocalDate endDate);

    Page<Battle> findByPlayerScoresUserIdAndStatus(String userId, String ended, Pageable pageable);
    @Aggregation(pipeline = {
            "{ $unwind: '$playerScores' }",
            "{ $group: { _id: '$subjectId', users: { $addToSet: '$playerScores.userId' } } }",
            "{ $project: { subjectId: '$_id', userCount: { $size: '$users' }, _id: 0 } }"
    })
    AggregationResults<Map> aggregateUsersBySubject();

    List<Battle> findByStatus(String ended);
}
