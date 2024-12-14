package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.TopicRequest;
import com.api.bee_smart_backend.helper.response.TopicResponse;

import java.util.List;
import java.util.Map;

public interface TopicService {


    Map<String, Object> getTopicsByGradeAndSemester(String grade, String semester, String page, String size, String search);

    TopicResponse createTopicByGradeId(String gradeId, TopicRequest request);

    TopicResponse updateTopicById(String topicId, TopicRequest request);

    TopicResponse getTopicById(String topicId);

    void deleteTopicById(String topicId);

    void deleteTopicsByIds(List<String> topicIds);
}
