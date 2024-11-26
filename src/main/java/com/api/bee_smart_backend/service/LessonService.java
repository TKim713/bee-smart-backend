package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;

import java.util.Map;

public interface LessonService {
    Map<String, Object> getAllLessons(String page, String size, String search);

    Map<String, Object> getListLessonByTopic(String topicId, int limit, int skip);

    LessonResponse createLessonByTopicId(String topicId, LessonRequest request);

    LessonResponse getLessonById(String lessonId);

    boolean isAuthenticated();
}
