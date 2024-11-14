package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;

import java.util.Map;

public interface LessonService {
    Map<String, Object> getListLessonByTopic(Long topicId, int limit, int skip);

    //Map<String, Object> getAllLessons(String page, String size, String search);

    LessonResponse createLessonByTopicId(Long topicId, LessonRequest request);

    LessonResponse getLessonById(Long lessonId);

    // Check if the user is authenticated based on the JWT token
    boolean isAuthenticated();
}
