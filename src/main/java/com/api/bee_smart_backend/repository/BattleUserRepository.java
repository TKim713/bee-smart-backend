package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Battle;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.model.dto.BattleUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BattleUserRepository extends MongoRepository<BattleUser, String> {
    Optional<BattleUser> findByUserAndDeletedAtIsNull(User user);
}
