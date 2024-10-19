package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}
