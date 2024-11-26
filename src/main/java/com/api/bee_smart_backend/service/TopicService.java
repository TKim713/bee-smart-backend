package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.TopicResponse;

import java.util.List;
import java.util.Map;

public interface TopicService {
    TopicResponse createTopicByChapterId(String chapterId, TopicRequest request);

    Map<String, Object> getListTopicByChapter(String chapterId, int limit, int skip);
}
