package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.TopicResponse;
import com.api.bee_smart_backend.model.Topic;

import java.util.List;
import java.util.Map;

public interface TopicService {

    Map<String, Object> getTopicsByGradeAndSemester(String gradeId, String semester, String page, String size);

    TopicResponse createTopicByGradeId(String gradeId, TopicRequest request);
}
