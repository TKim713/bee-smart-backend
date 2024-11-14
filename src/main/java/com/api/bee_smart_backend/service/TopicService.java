package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.TopicResponse;

import java.util.List;
import java.util.Map;

public interface TopicService {
    TopicResponse createTopicByChapterId(Long chapterId, TopicRequest request);

    Map<String, Object> getListTopicByChapter(Long chapterId, int limit, int skip);
}
