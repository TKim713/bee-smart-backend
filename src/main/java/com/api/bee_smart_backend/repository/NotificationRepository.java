package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Notification;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndReadFalse(User user);

    List<Notification> findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(User user);

    int countByUserAndReadFalseAndDeletedAtIsNull(User user);

    List<Notification> findByUserAndReadFalseAndDeletedAtIsNull(User user);
}
