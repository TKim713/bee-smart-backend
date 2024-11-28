package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.helper.response.LessonResponse;

import java.util.List;
import java.util.Map;

public interface LessonService {
    Map<String, Object> getAllLessons(String page, String size, String search);

    Map<String, Object> getListLessonByTopic(String topicId, String page, String size, String search);

    LessonResponse createLessonByTopicId(String topicId, LessonRequest request);

    LessonResponse updateLessonById(String lessonId, LessonRequest request);

    void deleteLessonById(String lessonId);

    void deleteLessonsByIds(List<String> lessonIds);

    LessonResponse getLessonById(String lessonId);

    boolean isAuthenticated();
}
