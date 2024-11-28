package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonDetailResponse;

import java.util.Map;

public interface LessonService {
    Map<String, Object> getAllLessons(String page, String size, String search);

    Map<String, Object> getListLessonByTopic(String topicId, String page, String size, String search);

    LessonDetailResponse createLessonByTopicId(String topicId, LessonRequest request);

    LessonDetailResponse updateLessonByTopicId(String topicId, String lessonId, LessonRequest request);

    void deleteLessonByTopicId(String topicId, String lessonId);

    LessonDetailResponse getLessonById(String lessonId);

    boolean isAuthenticated();
}
