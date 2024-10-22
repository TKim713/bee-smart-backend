package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.LessonRequest;
import com.api.bee_smart_backend.model.Lesson;

import java.util.Map;

public interface LessonService {
    Map<String, Object> getAllLessons(String page, String size, String search);

    Lesson createLesson(LessonRequest request);
}
